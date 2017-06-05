# Deploying Susi server with Kubernetes on Google Cloud


1. Go to [Google Cloud Free Trial](https://cloud.google.com/free-trial/) and sign up. You will get 300 dollars credit for 3 months.


2. Go to the console page: [Console](https://console.cloud.google.com/home)


3. If you don’t have any projects, a page that would guide you on how to start a project will pop up. Give your project a name.


4. In the search bar above on console page, enter 'Compute Engine' and click on ```Compute Engine```. 


5. If you have not activated billing for compute engine, click enable billing and choose your account to activate your Compute Engine. Your Google Compute Engine should be activated within a few minutes.

6. Select your computer zone region:
``` gcloud config set compute/zone us-central1-b ```

7. You can view your defauls/configure by this command:
```gcloud config list```

8. In the Web Console, enter:

```
sudo docker pull jyothiraditya/susi_server
```

9. Create a cluster

```gcloud container clusters create example-cluster```


10. Ensure kubectl has authentication credentials:

```gcloud auth application-default login```

11. Get docker image-id by running this command:

```docker images```

12. Then we need to store this docker image into our project:

``` docker tag <image-id> gcr.io/<project-id>/<image-id> ```

```gcloud docker -- push gcr.io/<project-id>/<image-id>```

11. query ```docker images``` . Copy or remember this id, we will need this id to tag the image.

```kubectl run susi-server --image=gcr.io/<project-id>/<image-id> --port=80```

12. Expose the container

```kubectl expose deployment susi-server --type=LoadBalancer --port=80```

13. Copy the external IP address for the hello-node app by typing this command.

```kubectl get service susi-server```

Wait for while and reenter query.Enter your assigned IP address with port 8080 (address:8080) into your browser to check if it is working.
