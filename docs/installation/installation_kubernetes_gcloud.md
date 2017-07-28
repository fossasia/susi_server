---
title: GCE Kubernetes
---

## Setup

- If you don’t already have a Google Account (Gmail or Google Apps), you must [create one](https://accounts.google.com/SignUp). Then, sign-in to Google Cloud Platform console ([console.cloud.google.com](http://console.cloud.google.com/)) and create a new project:


- Next, [enable billing](https://console.cloud.google.com/billing) in the Cloud Console in order to use Google Cloud resources and [enable the Container Engine API](https://console.cloud.google.com/project/_/kubernetes/list).

- Install [Docker](https://docs.docker.com/engine/installation/), and [Google Cloud SDK](https://cloud.google.com/sdk/).

- Finally, after Google Cloud SDK installs, run the following command to install `kubectl`:

    ```
    gcloud components install kubectl
    ```

- Choose a [Google Cloud Project zone](https://cloud.google.com/compute/docs/regions-zones/regions-zones) to run your service. We will be using us-central1-c. This is configured on the command line via:

    ```
    gcloud config set compute/zone us-central1-c
    ```

## Create and format a persistent data disk for data

- Create a persistent disk. (min. 1 GB) with a name `server-data-disk`.

    ```
    gcloud compute disks create server-data-disk --size 1GB
    ```

- The disk created is un formatted and needs to be formatted. To do that, we need to create a temporarily compute instance.

    ```
    gcloud compute instances create server-disk-formatter
    ```

- Wait for the instance to get created. Once done, attach the disk to that instance.

    ```
    gcloud compute instances attach-disk server-disk-formatter --disk server-data-disk
    ```

- SSH into the instance.

    ```
    gcloud compute ssh "server-disk-formatter"
    ```

- In the terminal, use the `ls` command to list the disks that are attached to your instance and find the disk that you want to format and mount

    ```
    ls /dev/disk/by-id
    ```

    ```
    google-example-instance       scsi-0Google_PersistentDisk_example-instance
    google-example-instance-part1 scsi-0Google_PersistentDisk_example-instance-part1
    google-[DISK_NAME]            scsi-0Google_PersistentDisk_[DISK_NAME]
    ```

    where `[DISK_NAME]` is the name of the persistent disk that you attached to the instance.

    The disk ID usually includes the name of your persistent disk with a `google-` prefix or a `scsi-0Google_PersistentDisk_` prefix. You can use either ID to specify your disk, but this example uses the ID with the `google-` prefix


- Format the disk with a single `ext4` filesystem using the `mkfs` tool. This command deletes all data from the specified disk.

    ```
    sudo mkfs.ext4 -F -E lazy_itable_init=0,lazy_journal_init=0,discard /dev/disk/by-id/google-[DISK_NAME]
    ```

- The disk is formatted and ready.
- Now exit the SSH session using `exit` command and Detach the disk from the instance by running

    ```
    gcloud compute instances detach-disk server-disk-formatter --disk server-data-disk
    ```

_You can delete the instance if your not planning to use it for anything else. But make sure the disk `server-data-disk` is not deleted._

## Create your Kubernetes Cluster

- Create a cluster via the `gcloud` command line tool:

    ```
    gcloud container clusters create susi-server-cluster
    ```

- Get the credentials for `kubectl` to use.

    ```
    gcloud container clusters get-credentials susi-server-cluster
    ```

## Pre deployment steps
- The Google persistent disk can be mounted as write to only one node at a time. So during the rolling update, if the new pod is being created on a different node, the disk doesn't get mounted.
- To prevent this we need to ensure that the server is restricted on one node only.

  ```
  kubect get nodes
  ```

  ```
  [node-name]                            Ready     1h        v1.6.4
  gke-susic-default-pool-0292dfcd-jbfm   Ready     1h        v1.6.4
  gke-susic-default-pool-0292dfcd-pwhp   Ready     1h        v1.6.4
  ```

- Choose any one node copy its `node name`

  ```
  kubectl label nodes [node-name] server=primary
  ```
- Ensure that your deployment config file has a nodeSelector `server:primary`

  ```
  spec:
  containers:
  - name: somthing
    image: image/something
  nodeSelector:
    server: primary
  ```

## Deploy our pods, services and deployments

- From the project directory, use the provided yaml configuration files that are in the `kubernetes` directory to deploy our application.

    ```
    kubectl create -R -f ./kubernetes/yamls/susi-server
    ```

- The Kubernetes master creates the load balancer and related Compute Engine forwarding rules, target pools, and firewall rules to make the service fully accessible from outside of Google Cloud Platform.
- Wait for a few minutes for all the containers to be created and the SSL Certificates to be generated and loaded.
- You can track the progress using the Web GUI as mentioned below. The namespace we used is `web`.

  ```
  kubectl get deployments --namespace=web
  ```

  ```
  kubectl get services --namespace=web
  ```

- Once deployed, your instance will be accessible at the `External IP` mentioned in the service.


## Other handy commands

- Delete all created pods, services and deployments

    ```
    kubectl delete -R -f ./kubernetes/yamls/susi-server
    ```

-  Access The Kubernetes dashboard Web GUI

    Run the following command to start a proxy.

    ```
    kubectl proxy
    ```

    and Goto [http://localhost:8001/ui](http://localhost:8001/ui)

- Deleting the cluster

    ```
    gcloud container clusters delete susi-server-cluster
    ```
