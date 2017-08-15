Steps to setup `susi` on cloud9
-------------------------------

* Create an account on [cloud9](https://c9.io/).
* Create a workspace with the relevant name. NOTE: Do not fill the URL with the clone URL of `susi_server`.
* Wait for the workspace to build and take you to the relevant page accordingly.
* Use the terminal in the page and type the following command to setup `susi_server` and run on cloud9.

```bash
wget -O - https://raw.githubusercontent.com/fossasia/susi_server/master/cloud9-setup.sh | bash
```
