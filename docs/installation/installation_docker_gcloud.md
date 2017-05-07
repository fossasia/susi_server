# Installation of loklak with Docker on Google Cloud


1. Go to [Google Cloud Free Trial](https://cloud.google.com/free-trial/) and sign up. You will get 300 dollars credit for 3 months.


2. Go to the console page: [Console](https://console.cloud.google.com/home)


3. If you don’t have any projects, a page that would guide you on how to start a project will pop up. Give your project a name.


4. In the search bar above on console page, enter 'Compute Engine' and click on ```Compute Engine```. 


5. If you have not activated billing for compute engine, click enable billing and choose your account to activate your Compute Engine. Your Google Compute Engine should be activated within a few minutes.


6. Click ```Create instance``` to create an instance. In machine type, choose ```small(1 shared vCPU)```.
	Choose whatever zone you like.
 
	In Boot Disk, choose ```Ubuntu 14.04```, and the disk size should be larger than 40GB.
	On the bottom of the page, click ```Management, disk, networking, access & security options``` to show more options. Inside of this, click into the tab ```Networking``` and choose ```New static IP``` instead of ```Ephemeral```. Enter a name for your IP. Google will assign a IP for you.
	Check the two boxes ```Allow HTTP traffic``` and ```Allow HTTPS traffic```. Finally, click ```Create``` to create an instance. Wait a few minutes for the creation to complete.


7. Once the creation has finished, click ssh below to establish web ssh connections.

8. In the Web Console, enter:

```
sudo apt-get update
sudo apt-get upgrade
sudo apt-get -y install docker.io
sudo fallocate -l 4G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
sudo docker build https://github.com/loklak/loklak_server.git
```

Once finished, the last line will provide the image ID, like ```Successfully built 4e11208a7b34```. Copy or remember this id, we will need this id to tag the image.
Tag your image by entering ```sudo docker tag YOUR_IMAGE_ID loklak```. In my case, we enter ```sudo docker tag 4e11208a7b34 loklak```
Run our server by entering:
```
sudo docker run -d -p 80:80 -p 443:443 loklak
```
Enter your assigned IP address into your browser to check if it is working.

## Installing loklak on DigitalOcean with Docker

[DigitalOcean](https://www.digitalocean.com) - simple cloud hosting, built for developers.

1. Register in [DigitalOcean](https://www.digitalocean.com) and get 10$ credit.

2. Click 'Create droplet' button, choose your droplet image (Docker), size, location and add your SSH keys.

   ![Droplet image](http://i.imgur.com/wXXvg7W.png)

   ![SSH keys](http://i.imgur.com/egW1HsV.png)

3. Click 'Create', wait a minute, copy ip of your new droplet and login to it using SSH: 
   ```bash
   ssh root@YOUR.DROPLET.IP
   ```

4. Docker is already installed, you can check it using ```docker version``` command. You should see something like this:
   ```
   root@sevazhidkov:~# docker version
   Client:
    Version:      1.9.1
    API version:  1.21
    Go version:   go1.4.2
    Git commit:   a34a1d5
    Built:        Fri Nov 20 13:12:04 UTC 2015
    OS/Arch:      linux/amd64
   
   Server:
    Version:      1.9.1
    API version:  1.21
    Go version:   go1.4.2
    Git commit:   a34a1d5
    Built:        Fri Nov 20 13:12:04 UTC 2015
    OS/Arch:      linux/amd64
   ```
5. Pull Docker image from [Loklak repository](https://hub.docker.com/r/mariobehling/loklak/) in Docker Hub (it should take about a minute):
   ```bash
   docker pull mariobehling/loklak
   ```

6. OK, you're ready to run Loklak:
   ```bash
   docker run -d -p 80:80 -p 443:443 mariobehling/loklak:latest
   ```

7. Go to your droplet IP using web browser. You should see Loklak main page.
