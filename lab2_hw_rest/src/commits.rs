use crate::{
    providers::{get_commit_provider, parse_repo_uri, Commit, CommitProvider, Repo},
    result::{ErrorKind, Result},
};

pub async fn get_commits(author: &str, repos: &Vec<String>) -> Result<Vec<Commit>> {
    let mut repo_provider = Vec::<(Repo, Box<dyn CommitProvider>)>::new();

    for repo_url in repos {
        let repo = parse_repo_uri(repo_url)?;

        if let Some(provider) = get_commit_provider(&repo.host) {
            repo_provider.push((repo, provider));
        } else {
            let repo = repo_url.to_owned();
            eprintln!("Unable to find provider");
            return Err(ErrorKind::InvalidParameterValue("repo", repo).into());
        }
    }

    let mut jobs = vec![];
    for (repo, provider) in repo_provider {
        jobs.push(async move { provider.get_recent_commits(repo, author).await });
    }

    let jobs = futures::future::join_all(jobs).await;
    let mut commits = vec![];

    for job in jobs {
        let mut job = job?;
        commits.append(&mut job);
    }

    Ok(commits)
}
