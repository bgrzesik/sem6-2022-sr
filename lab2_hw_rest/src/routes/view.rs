use rocket::{form::Form, http::ContentType};

use crate::{
    commits::get_commits,
    result::{Error, ErrorKind, Result},
};

pub fn get_routes() -> impl Into<Vec<rocket::Route>> {
    rocket::routes![index, fetch]
}

#[derive(rocket::FromForm, serde::Deserialize)]
struct FetchRequest {
    author: Option<String>,
    repos: Option<String>,
}

#[rocket::post("/fetch", data = "<req>")]
async fn fetch(req: Form<FetchRequest>) -> Result<(ContentType, String)> {
    let author = req
        .author
        .as_ref()
        .map(|s| s.trim())
        .filter(|a| !a.is_empty())
        .ok_or(Error::from(ErrorKind::InvalidParameter("Author")))?;

    let repos: Vec<String> = req
        .repos
        .as_ref()
        .ok_or(Error::from(ErrorKind::InvalidParameter("Repositories")))?
        .split("\n")
        .map(|s| s.trim())
        .filter(|s| !s.is_empty())
        .map(ToOwned::to_owned)
        .collect();

    if repos.is_empty() {
        return Err(ErrorKind::InvalidParameter("Repositories").into());
    }

    let commits = get_commits(author, &repos)
        .await?
        .into_iter()
        .map(|commit| {
            let sha = commit.sha.unwrap_or("no sha".to_owned());
            let author = commit.author.unwrap_or("no author".to_owned());
            let mut subject = commit.subject.unwrap_or("no subject".to_owned());
            subject = subject.replace("\n", "<br/>");

            format!(
                r#"
                <tr>
                    <td>{sha}</td>
                    <td>{author}</td>
                    <td>{subject}</td>
                </tr>
            "#
            )
        })
        .fold(String::new(), |left, right| left + &right);

    let html = format!(
        r#"
    <!DOCTYPE html>
    <html>
        <body>
            <h1> Commit fetcher </h1>
            <table border="1">
                <tr>
                    <th> <b> SHA </b> </th>
                    <th> <b> Author </b> </th>
                    <th> <b> Message </b> </th>
                </tr>
                {commits}
            </table>
        </body>
    </html>
    "#
    );

    Ok((ContentType::HTML, html))
}

#[rocket::get("/", format = "text/html")]
fn index() -> (ContentType, String) {
    let html = format!(
        r#"
    <!DOCTYPE html>
    <html>
        <body>
            <h1> Commit fetcher </h1>
            <form method="POST" action="/fetch">

                <label for="author">Author</label>
                </br>
                <input id="author" name="author" type="text"/>
                <br/>

                <label for="Repos">Repositories URL</label>
                </br>
                <textarea id="repos" name="repos" type="">
                </textarea>
                <br/>

                <input type="submit"/>
            </form>
        </body>
    </html>
    "#
    );

    (ContentType::HTML, html)
}
