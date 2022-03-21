use crate::commits::get_commits;
use crate::providers::Commit;
use crate::result::{ErrorKind, Result};

use rocket::serde::json::Json;

pub fn get_routes() -> impl Into<Vec<rocket::Route>> {
    rocket::routes![by_author]
}

#[derive(Debug, serde::Serialize)]
struct Res {
    ip: String,
}

#[rocket::get("/by_author?<author>&<repo>")]
async fn by_author(author: &str, repo: Vec<String>) -> Result<Json<Vec<Commit>>> {
    // Move to repos variable for code readability
    let repos = repo;

    if author.is_empty() {
        return Err(ErrorKind::InvalidParameter("author").into());
    }
    if repos.is_empty() {
        return Err(ErrorKind::InvalidParameter("repo").into());
    }

    let commits = get_commits(author, &repos).await?;

    Ok(commits.into())
}
