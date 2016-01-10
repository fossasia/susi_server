# Installation of loklak with Docker

## Installing loklak on Bluemix with Docker

1. Install docker on your system

2. Get a Bluemix Account with this URL: http://ibm.biz/joinbluemix <br>
2.1 Press Sigup and type in your credentials <br>
2.2 Check your email and press the validation link <br>
2.3 Sign in to bluemix

3. Make sure you are in US South. The URL is https://console.ng.bluemix.net 
(beside US South there is London and Sydney)

4. Press Dashboard and create a space called dev

5. Install cloud foundry command line tools for your OS from [here](https://github.com/cloudfoundry/cli/releases)
   In Linux:
   ```
   dpkg -i cf-cli-version.dep
   ```
6. Install ic plugin as described [here](https://www.ng.bluemix.net/docs/containers/container_cli_ov.html#container_cli_cfic_install)
   Linux:
   ```
   cf install-plugin https://static-ice.ng.bluemix.net/ibm-containers-linux_x64
   ```
7. Login to bluemix with 
   ```
   cf login
   API endpoint: api.ng.bluemix.net
   ```
8. Login to docker on bluemix with
cf ic login   (No credentials are necessary of you logged in to bluemix before

9. Create namespace with
   ```
   cf ic namespace set <your namespace like hugo, make sure the name is unique>
   
   root@44ee147e1aa5:/# cf ic namespace set hugo
   FAILED
   {
     "code": "IC5090E",
     "description": "Cannot assign namespace hugo to org 8b826387-9960-48d6-a409-1c5347b937af. Please ensure the namespace is not already in use.",
     "incident_id": "df24a7aedde48fbb",
     "name": "NamespaceToOrgAssignError",
     "rc": "409",
     "type": "Infrastructure" 
   }

   root@44ee147e1aa5:/# cf ic namespace set ottohttps://codein.withgoogle.com/tasks/5485387342413824/
   otto
   ```
10. Init your docker connection with `cf ic init`

11. Upload the loklak docker file to your namespace with `cf ic cpi mariobehling/loklak loklak`   (takes some time)

12. Create docker group with: (the XXXX must be unique, play around to found a free name)
   ```
   cf ic group create --name loklak --desired 2 -m 1024 -n XXXX -d mybluemix.net -p 80 registry.ng.bluemix.net/<namespace>/loklak
   ```
13. Check if your group is running either with pressing Dashboard in the browser or:
   ```
   cf ic group list
   ```
14. Wait until your container group is build and the network is configured (>1 minute) and
   ```
   check at https://XXXX.mybluemix.net is working with your version of loklak
   ```
15. Send your own bluemix loklak link to Mario in order to prove your done it

## Installing loklak on AWS with Docker

Add step by step information here.

## Installing loklak on Google Cloud with Docker


1.  Go to [Google Cloud Free Trial](https://cloud.google.com/free-trial/) and sign up. You will get 300 dollars credit for 3 month.


2. Go to the console page: [Console](https://console.cloud.google.com/home)


3. If you don’t have any project, it will pop up a page and guide you to start a project. Give a name to your project.


4.  In the search bar above on console page, enter ```Compute Engine``` and click into Compute Engine. 


5. If you do not activate billing for compute engine, click enable billing and choose your account to activate your Compute Engine.Your Google Compute Engine should be activated within a few minutes.


6. Click to create a instance, in machine type, choose ```small(1 shared vCPU)```.
 Choose whatever zone you like.
 
 In Boot Disk, choose ```Ubuntu 14.04```, and the disk size should be larger than 40GB.
On the bottom of the page, click ```Management, disk, networking, access & security options``` to show more options. Inside of this, click into networking and choose new static IP instead of ephemeral. Enter a name for your IP. Google will assign a IP for you.
Click allow http and https traffic. Finally click create to create a instance. Wait a few minutes for creation to complete.


7. Once creation finished, click ssh bottom to establish a web ssh connections.

8. In the Web Console, enter:


```
sudo apt-get update
sudo apt-get upgrade
apt-get -y install docker.io
sudo docker build https://github.com/loklak/loklak_server.git
```

Once finished, the last line will provide the image ID, like ```Successfully built 4e11208a7b34```. Copy or remember this id, we will need this id to tag the image.
Tag your image by enter ```sudo docker tag YOUR_IMAGE_ID loklak```. In my case, we enter ```sudo docker tag 4e11208a7b34 loklak```
   Run our server by enter:
```
sudo docker run -d -p 80:80 -p 443:443 loklak
```
Enter your assigned IP to browser to check if it is working.
