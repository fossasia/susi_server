## Setup:

- If you don&#39;t already have a Google Account (Gmail or Google Apps), you must [create one](https://accounts.google.com/SignUp). Then, sign-in to Google Cloud Platform console ( [cloud.google.com](http://console.cloud.google.com/)) and create a new project.

- Subscribe [free tier](https://cloud.google.com/free/) for 12 months.Next, [enable billing](https://console.cloud.google.com/billing) in the Cloud Console in order to use Google Cloud resources and [enable the Container Engine API](https://console.cloud.google.com/project/_/kubernetes/list).

- Install [Docker](https://docs.docker.com/engine/installation/), and [Google Cloud SDK](https://cloud.google.com/sdk/).
- Finally, after Google Cloud SDK installs, run the following command to install kubectl:
                gcloud components install kubectl

- Choose a [Google Cloud Project zone](https://cloud.google.com/compute/docs/regions-zones/regions-zones) to run your service. We will be using us-central1. This is configured on the command line via:

                gcloud config set compute/zone us-central1


## Registering a Domain and Pre Deployment Steps: 

- You can register free domain at [http://www.freenom.com](http://www.freenom.com/).Next, you have to set IP for DNS of this domain.

- Reserve static IP address with this command:

                gcloud compute addresses create IPname --region us-central1

- You will get a created message. To see your IP go to VPC Network -&gt; External IP addresses.

- Add this IP to DNS zone of your domain and to the kubernetes/yamls/nginx/service.yaml file for &quot;loadBalancerIP&quot; parameter.

- Change enviroment variavles in kubernetes/yamls/application/deployment.yaml

- Replace domain name in kubernetes/yamls/application/ingress-notls.yaml and kubernetes/yamls/application/ingress-tls.yaml with your domain name.

- Add your email ID to kubernetes/yamls/lego/configmap.yaml for &quot;lego.email&quot; parameter.


## Deployment:

- First create a cluster

                gcloud container clusters create clusterName

- In gcloud shell run the following command to deploy application using given configurations.

                bash ./kubernetes/deploy.sh create all

-  This will create the deployment as we have defined in the script.

- The Kubernetes master creates the load balancer and related Compute Engine forwarding rules, target pools, and firewall rules to make the service fully accessible from outside of Google Cloud Platform.

- Wait for a few minutes for all the containers to be created and the SSL Certificates to be generated and loaded.

## Tracking:

- To track the progress using the Web GUI run

                kubectl proxy

- After that goto [http://localhost:8001/ui](http://localhost:8001/ui)

## Cleanup:

- If you want to delete the deployment from the cluster enter this command:

                bash ./kubernetes/deploy.sh delete all


- To delete cluster enter:

                gcloud container clusters delete clusterName
