# Installation of Susi server with Docker on Bluemix

1. Install Docker on your system

2. Get a Bluemix Account with this URL: http://ibm.biz/joinbluemix <br>
    2.1 Press Signup and type in your credentials <br>
    2.2 Check your email and press the validation link <br>
    2.3 Sign into bluemix

3. Make sure you are in US South. The URL is https://console.ng.bluemix.net 
(besides US South there is London and Sydney)

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
   ```
   API endpoint is api.ng.bluemix.net

8. Login to Docker on bluemix with

	```
	cf ic login
	```
	(No credentials are necessary if you logged into bluemix before)

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

11. Upload the susi server docker file to your namespace with

	`cf ic cpi fossasia/susi_server susi_server`   (takes some time)

12. Create docker group with: (the XXXX must be unique, play around to found a free name)

   ```
   cf ic group create --name susi_server --auto --desired 2 -m 1024 -n XXXX -d mybluemix.net -p 80 registry.ng.bluemix.net/<namespace>/susi_server
   ```

13. Check if your group is running either with pressing Dashboard in the browser or:

   ```
   cf ic group list
   ```

14. Wait until your container group is build and the network is configured (>1 minute) and

   ```
   check at https://XXXX.mybluemix.net is working with your version of susi server
   ```

15. Send your own bluemix susi server link to Mario in order to prove you done it

