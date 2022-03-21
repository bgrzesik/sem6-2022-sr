use crate::result::{ErrorKind, Result};
use futures::Future;
use rocket::http::uri::Uri;

#[derive(serde::Deserialize, serde::Serialize, Debug, Default)]
pub struct Commit {
    pub author: Option<String>,
    pub sha: Option<String>,
    pub subject: Option<String>,
}

pub type ProviderOutput<'a> = impl Future<Output = Result<Vec<Commit>>> + 'a;
pub trait CommitProvider: Sync + Send {
    // type Output: Future<Output = Result<Vec<Commit>>> + 'a;

    fn get_recent_commits<'a, 's: 'a>(&'a self, repo: Repo, author: &'s str) -> ProviderOutput<'a>;
}

mod github;
pub use github::Github;

pub fn get_commit_provider(host: &str) -> Option<Box<dyn CommitProvider>> {
    match host {
        "github.com" => Some(box Github),
        _ => None,
    }
}

#[derive(Debug)]
pub struct Repo {
    pub host: String,
    pub owner: Option<String>,
    pub name: Option<String>,
}

pub fn parse_repo_uri(url: &str) -> Result<Repo> {
    match Uri::parse_any(url) {
        Ok(Uri::Authority(a)) => {
            return Ok(Repo {
                host: a.host().to_owned(),
                owner: None,
                name: None,
            });
        }
        Ok(Uri::Absolute(ref a)) if a.authority().is_some() => {
            let authority = a.authority().unwrap();

            let mut segments = a.path().segments();
            let owner = segments.next().map(ToOwned::to_owned);
            let name = segments.next().map(ToOwned::to_owned);

            return Ok(Repo {
                host: authority.host().to_owned(),
                owner,
                name,
            });
        }
        Err(err) => {
            eprintln!("{url} {err:?}");
            return Err(ErrorKind::RepoParseError(url.to_owned()).into());
        }
        _ => {
            return Err(ErrorKind::RepoParseError(url.to_owned()).into());
        }
    };
}
