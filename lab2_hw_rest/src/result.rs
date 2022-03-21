use rocket::{
    http::{Accept, ContentType},
    Response,
};

use error_chain;

error_chain::error_chain! {
    types {
        Error, ErrorKind, ResultExt, Result;
    }
    errors {
        NotFound {
            display("Not found"),
            description("Given resource could not have been found"),
        }
        InternalError {
            display("Internal server error"),
            description("Unexpected internal error"),
        }
        MarshalError {
            display("Marshalling error"),
            description("Data malfolded"),
        }
        InvalidParameter(param: &'static str) {
            display("Invalid Parameter `{param}`"),
            description("Invalid data has been supplied"),
        }
        InvalidParameterValue(param: &'static str, value: String) {
            display("Invalid parameter value `{param}` = `{value}`"),
            description("Invalid data has been supplied"),
        }
        RepoParseError(repo: String) {
            display("Invalid repo URL {repo} supplied")
            description("Invalid repo URL supplied")
        }
        MissingUpstreamBranch(repo: String) {
            display("Missing upstream branch for {repo}")
            description("No 'main', 'master', 'upstream' has been found in the given repository")
        }
    }
}

impl From<&ErrorKind> for rocket::http::Status {
    fn from(err: &ErrorKind) -> Self {
        use rocket::http::Status;
        use ErrorKind::*;

        match err {
            NotFound => Status::NotFound,

            InternalError => Status::InternalServerError,
            MarshalError => Status::InternalServerError,

            InvalidParameter(_) => Status::BadRequest,
            InvalidParameterValue(_, _) => Status::BadRequest,
            RepoParseError(_) => Status::BadRequest,
            MissingUpstreamBranch(_) => Status::NotFound,

            Msg(_) => Status::InternalServerError,
            _ => Status::InternalServerError,
        }
    }
}

impl<'r, 'o: 'r> rocket::response::Responder<'r, 'o> for Error {
    fn respond_to(self, req: &'r rocket::Request<'_>) -> rocket::response::Result<'o> {
        let (content_ty, content) = if req.accept() == Some(&Accept::JSON) {
            let json = serde_json::json!({
                "error": format!("{}", self.kind()),
                "description": self.description(),
            });

            (ContentType::JSON, json.to_string())
        } else {
            let error = format!("{}", self.kind());
            let description = self.description();

            let html = format!(
                r#"
            <!DOCTYPE html>
            <html>
                <body>
                    <h1> Commit fetcher </h1>
                    <h2> <b> Error: </b> {error} </h2>
                    <h3> <b> Description: </b> {description} </h3>
                </body>
            </html>
            "#
            );

            (ContentType::HTML, html.to_string())
        };

        Response::build()
            .header(content_ty)
            .status(self.kind().into())
            .sized_body(content.len(), std::io::Cursor::new(content))
            .ok()
    }
}
