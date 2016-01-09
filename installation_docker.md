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

Add step by step information here.
