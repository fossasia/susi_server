## Setup:

- If you don&#39;t already have a Google Account (Gmail or Google Apps), you must [create one](https://accounts.google.com/SignUp). Then, sign-in to Google Cloud Platform console ( [cloud.google.com](http://console.cloud.google.com/)) and create a new project.

- Subscribe [free tier](https://cloud.google.com/free/) for 12 months.Next, [enable billing](https://console.cloud.google.com/billing) in the Cloud Console in order to use Google Cloud resources and [enable the Container Engine API](https://console.cloud.google.com/project/_/kubernetes/list).

- Install [Docker](https://docs.docker.com/engine/installation/), and [Google Cloud SDK](https://cloud.google.com/sdk/).
- Finally, after Google Cloud SDK installs, run the following command to install kubectl:
                gcloud components install kubectl

- Choose a [Google Cloud Project zone](https://cloud.google.com/compute/docs/regions-zones/regions-zones) to run your service. We will be using us-central1. This is configured on the command line via:

                gcloud config set compute/zone us-central1


## Deployment:

- First create a cluster

                gcloud container clusters create clusterName

- In gcloud shell run the following command to deploy application using given configurations.

                bash ./kubernetes/deploy.sh create

-  This will create the deployment according to yaml files defined.

- The Kubernetes master creates the load balancer and related Compute Engine forwarding rules, target pools, and firewall rules to make the service fully accessible from outside of Google Cloud Platform.

- Wait for a few minutes for all the containers to be created.

## Tracking:

- To track the progress using the Web GUI run

                kubectl proxy

- After that goto [http://localhost:8001/ui](http://localhost:8001/ui)

## Cleanup:

- If you want to delete the deployment from the cluster enter this command:

                bash ./kubernetes/deploy.sh delete


- To delete cluster enter:

                gcloud container clusters delete clusterName
