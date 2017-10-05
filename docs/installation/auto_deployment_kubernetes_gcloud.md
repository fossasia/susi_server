# Auto Deployment of SUSI Server using Kubernetes on Google Cloud Platform

SUSI Server is Auto Deployed on Google Cloud Platform using Kubernetes and Docker Images after each commit in the GitHub repo with the help of Travis Continuous Integration. Whenever a new commit is added to the repo, during the Travis build, we build the docker image of the server and then use it to deploy the server on Google Cloud Platform.

### Prerequisites

 - You must be signed in to your Google Cloud Account and have enabled billing and must have credits left in your account.
 - You must have a docker account and a repository in it.
 - You should have enabled Travis on your repo and have a Travis.yml file in your repo.
 - You must already have a project in Google Cloud Console.

### Pre Deployment Steps

You will be needed to do some work on Google Cloud Platform before actually starting the auto deployment process. Those are:

 - Creating a new Cluster.
 - Adding and Formatting Persistence Disk
 - Adding a Persistent Volume CLaim (PVC)
 - Labeling a node as primary.

Please refer to [this documentation](https://github.com/fossasia/susi_server/blob/afb00cd9c421876f5d640ce87941e502aa52e004/docs/installation/installation_kubernetes_gcloud.md) for more details.

### Implementation

1. Configure Travis.yml to call the deploy script where we will add code for updating kubernetes deployment and docker image.

```
after_success:
- bash kubernetes/travis/deploy.sh
```

2. In the deploy script, remove obsolete Google Cloud files and install Google Cloud SDK and kubectl command. Use following lines to do that.

```
echo ">>> Removing obsolete gcoud files"
sudo rm -f /usr/bin/git-credential-gcloud.sh
sudo rm -f /usr/bin/bq
sudo rm -f /usr/bin/gsutil
sudo rm -f /usr/bin/gcloud

echo ">>> Installing new files"
curl https://sdk.cloud.google.com | bash;
source ~/.bashrc
gcloud components install kubectl
```

3. Download the JSON file which contains your Google Cloud Credentials, then copy that file to the repository after encrypting it using Travis encryption keys. Follow https://youtu.be/7U4jjRw_AJk this video to see how to do that.

4. After adding your encrypted credentials.json files in the repository, use those credentials to login into your google cloud account. First decrypt your credentials, then login into your account and set the project you already created earlier.

```
echo ">>> Decrypting credentials and authenticating gcloud account"
# Decrypt the credentials we added to the repo using the key we added with the Travis command line tool
openssl aes-256-cbc -K $encrypted_YOUR_key -iv $encrypted_YOUR_iv -in ./kubernetes/travis/Credentials.json.enc -out Credentials.json -d
gcloud auth activate-service-account --key-file Credentials.json
export GOOGLE_APPLICATION_CREDENTIALS=$(pwd)/Credentials.json
#add gcoud project id
gcloud config set project YOUR_PROJECT_ID
gcloud container clusters get-credentials YOUR_CONTAINER
```

5. Now, we have logged into Google Cloud, we need to build docker image from the [dockerfile]((https://github.com/fossasia/susi_server/blob/development/kubernetes/images/Dockerfile)). Add “\$DOCKER_USERNAME” and “\$DOCKER_PASSWORD” as environment variables in Travis settings of the repository.

```
echo ">>> Building Docker image"
cd kubernetes/images

docker build --no-cache -t YOUR_DOCKER_USERNAME/YOUR_DOCKER_REPO:$TRAVIS_COMMIT .
docker login -u="$DOCKER_USERNAME" -p="$DOCKER_PASSWORD"
docker tag YOUR_DOCKER_USERNAME/YOUR_DOCKER_REPO:$TRAVIS_COMMIT YOUR_DOCKER_USERNAME/YOUR_DOCKER_REPO:latest
```

6. Push the docker image created in previous step and update the deployment.

```
echo ">>> Pushing docker image"
docker push YOUR_DOCKER_USERNAME/YOUR_DOCKER_REPO

echo ">>> Updating deployment"
kubectl set image deployment/YOUR_CONTAINER_NAME --namespace=default YOUR_CONTAINER_NAME=YOUR_DOCKER_USERNAME/YOUR_DOCKER_REPO:$TRAVIS_COMMIT
```

### Resources

 - The documentation for setting up your project on Google Cloud Console before starting auto deployment https://github.com/fossasia/susi_server/blob/afb00cd9c421876f5d640ce87941e502aa52e004/docs/installation/installation_kubernetes_gcloud.md
 - The documentation for encrypting your google cloud credentials and adding them to your repo https://cloud.google.com/solutions/continuous-delivery-with-travis-ci
 - Docs for Docker to get you started with Docker https://docs.docker.com/
 - Travis Documentation on how to secure your credentials https://docs.travis-ci.com/user/encryption-keys/
 - Travis Documentation on how to add environment variables in your repo settings https://docs.travis-ci.com/user/environment-variables/
