# Development Workflow

## Fixing issues


### Step 1: Pick an issue to fix
 
 After selecting the issue
 
  1.Comment on the issue saying you are working on the issue.

  2.We except you to discuuss the approach either by commenting or in the gitter.
  
  3.Updates or progress on the issue would be nice.

### Step 2: Branch policy<br>

 Start off from your `development` branch and make sure it is up-to-date with the latest version of the committer repo's
 `development` branch. Make sure you are working in development branch only.<br>
 `git pull upstream development`
    
  If you have not added upstream follow the steps given [here](https://help.github.com/articles/configuring-a-remote-for-a-fork/).
  
### Step 3: Coding Policy

  * Please help us follow the best practice to make it easy for the reviewer as well as the contributor. 
    We want to focus on the code quality more than on managing pull request ethics.
    
  * Single commit per pull request

  * Reference the issue numbers in the commit message. Follow the pattern Fixes #

  * Follow uniform design practices. The design language must be consistent throughout the app.

  * The pull request will not get merged until and unless the commits are squashed. 
    In case there are multiple commits on the PR,  the commit author needs to squash them and 
    not the maintainers cherrypicking and merging squashes.
  
  * If you don't know what does squashing of commits is read from [here](http://stackoverflow.com/a/35704829/6181189).

  * If the PR is related to any front end change, please attach relevant screenshots in the pull request description

### Step 4: Submitting a PR

 Once a PR is opened, try and complete it within 2 weeks, or at least stay actively working on it.
 Inactivity for a long period may necessitate a closure of the PR. As mentioned earlier updates would be nice.

### Step 5: Code Review
 Your code will be reviewed, in this sequence, by:

 * Travis CI: by building and running tests.<br>
   If there are failed tests, the build will be marked as a failure.
   You can consult the CI log to find which tests.<br>
   Ensure that all tests pass before triggering another build.
 * The CI log will also contain the command that will enable running the failed tests locally.<br>
 * Reviewer: A core team member will be assigned to the PR as its reviewer, who will approve your PR or he will suggest changes.

