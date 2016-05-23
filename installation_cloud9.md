Steps to setup loklak on cloud9
-------------------------------

* Create a cloud9 account
* Create a workspace with the relevant name (Do not fill the url with the clone url of loklak server)
* Wait for the workspace to build and take you to the relevant page
* Use the terminal in the page and type the following command to setup the loklak server and run on cloud9
```bash
wget -O - https://raw.githubusercontent.com/loklak/loklak_server/master/cloud9-setup.sh | bash
```
