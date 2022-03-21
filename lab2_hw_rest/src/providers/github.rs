use super::{CommitProvider, ProviderOutput, Repo};
use crate::result::{Error, ErrorKind, Result};

const API_GITHUB_HOST: &str = "https://api.github.com";

static APP_USER_AGENT: &str = concat!(env!("CARGO_PKG_NAME"), "/", env!("CARGO_PKG_VERSION"));

#[allow(dead_code)]
#[derive(serde::Deserialize)]
struct BranchCommit {
    sha: Option<String>,
    url: Option<String>,
}

#[allow(dead_code)]
#[derive(serde::Deserialize)]
struct Branch {
    name: Option<String>,
    commit: Option<BranchCommit>,
    protected: Option<bool>,
}

#[derive(serde::Deserialize)]
struct Author {
    name: Option<String>,
    email: Option<String>,
}

#[derive(serde::Deserialize)]
struct Ref {
    sha: Option<String>,
    commit: Option<Commit>,
}

#[allow(dead_code)]
#[derive(serde::Deserialize)]
struct Commit {
    author: Option<Author>,
    committer: Option<Author>,
    message: Option<String>,
}

fn get_branch<'a>(name: &str, branches: &'a Vec<Branch>) -> Option<&'a Branch> {
    branches.iter().find(|branch| {
        if let Some(ref branch_name) = branch.name {
            if branch_name == name {
                return true;
            }
        }

        false
    })
}

impl Into<super::Commit> for Ref {
    fn into(self) -> super::Commit {
        let mut commit = super::Commit {
            sha: self.sha,
            ..Default::default()
        };

        if let Some(c) = self.commit {
            if let Some(a) = c.author {
                let name = a.name.unwrap_or_else(String::new);
                let email = a.email.unwrap_or_else(String::new);

                commit.author = Some(format!("{} <{}>", name, email));
            }
            commit.subject = c.message;
        }

        commit
    }
}

pub struct Github;

impl Github {
    async fn get_recent_commits_async<'s>(
        &self,
        repo: Repo,
        author: &str,
    ) -> Result<Vec<super::Commit>> {
        if repo.owner.is_none() || repo.name.is_none() {
            return Err(ErrorKind::RepoParseError("".to_owned()).into());
        }

        let owner = repo.owner.unwrap();
        let name = repo.name.unwrap();

        let client = reqwest::Client::builder()
            .user_agent(APP_USER_AGENT)
            .build()
            .map_err(|err| {
                eprintln!("Request error {err:?}");
                Error::from(ErrorKind::InternalError)
            })?;

        let url = format!("{API_GITHUB_HOST}/repos/{owner}/{name}/branches");
        println!("Requesting {url}");

        let branches: Vec<Branch> = client
            .get(url)
            .send()
            .await
            .map_err(|err| {
                eprintln!("Request error {err:?}");
                Error::from(ErrorKind::InternalError)
            })?
            .json()
            .await
            .map_err(|err| {
                eprintln!("Request error (json) {err:?}");
                Error::from(ErrorKind::MarshalError)
            })?;

        let sha = get_branch("main", &branches)
            .or_else(|| get_branch("master", &branches))
            .or_else(|| get_branch("upstream", &branches))
            .and_then(|b| b.commit.as_ref())
            .and_then(|c| c.sha.as_ref())
            .ok_or_else(|| {
                let repo = format!("{}/{}/{}", repo.host, owner, name);
                Error::from(ErrorKind::MissingUpstreamBranch(repo))
            })?;

        let url = format!(
            "{API_GITHUB_HOST}/repos/{owner}/{name}/commits?per_page=10&sha={sha}&author={author}"
        );
        println!("Requesting {url}");
        let commits: Vec<Ref> = client
            .get(url)
            .send()
            .await
            .map_err(|err| {
                eprintln!("Request error {err:?}");
                Error::from(ErrorKind::InternalError)
            })?
            .json()
            .await
            .map_err(|err| {
                eprintln!("Request error (json) {err:?}");
                Error::from(ErrorKind::MarshalError)
            })?;

        Ok(commits.into_iter().map(Into::into).collect())
    }
}

impl CommitProvider for Github {
    fn get_recent_commits<'a, 's: 'a>(&'a self, repo: Repo, author: &'s str) -> ProviderOutput {
        Box::pin(self.get_recent_commits_async(repo, author))
    }
}
