# Installation of Susi server with Docker on AWS

1. Sign in to [AWS](https://aws.amazon.com)

2. In the top-right corner, choose a area you want to set up the instance, e.g.: Asia Pacific (Singapore).

3. In the console panel, choose EC2. In EC2 Control Panel, choose key pairs, under Network & Security, and create a new key pair. It will automatically download the private key, and you should do ```chmod 400 YOUR_KEY_NAME``` to prevent other user to access your private key. After that, go back to your dashboard, and click Launch Instance.

4. In Step 1: Choose an Amazon Machine Image, we choose ```Ubuntu Server 14.04 LTS (HVM), SSD Volume Type```.

5. In Step 2: Choose an Instance Type, we choose ```t2.micro```(<strong>Warning:</strong> ```t2.micro``` is not suitable for running Susi for a long period, and AWS may shut down your instance(block your Internet access on specific port) at any time. ```t2.micro``` instance users are only allow to use <strong>10%</strong> of a single CPU core on average. In our case,  it might use 30% - 40% on an average. Reference: [Burstable Instance](http://aws.amazon.com/ec2/faqs/#burst).

6. Then we click ```Configure Instance Details```, do not create a instance yet.

7. Under ```Auto-assign Public IP```, we choose ```Enable```

8. Then we click ```Next: Add Storage```

9. We modify the storage to ```30GiB``` instead of default ```8Gib```

10. Click ```Next``` twice to go to ```Step 6: Configure Security Group```. Under this, we choose ```All TCP``` for type and ```Anywhere``` for source.

11. We are ready to launch, click ```Review and Launch```, if everything is correct, click ```Launch```. It will ask you to choose a key pair, choose the one we just created.

12. We go back to EC2 control panel again. Click on instances on the left hand side. Then choose the instance you just created, and click connect button on top. It will you connect to your EC2 by giving you a example like:
	```
	ssh -i "mykey.pem" ubuntu@ec2-54-169-103-75.ap-southeast-1.compute.amazonaws.com
	```

13. Once connected, we have to set up docker and add a 4G swapfile to prevent lack of memory:
	```
	sudo apt-get update
	sudo apt-get -y upgrade
	sudo fallocate -l 4G /swapfile
	sudo chmod 600 /swapfile
	sudo mkswap /swapfile
	sudo swapon /swapfile
	sudo apt-get install linux-image-extra-`uname -r`
	sudo apt-key adv --keyserver hkp://pgp.mit.edu:80 --recv-keys 58118E89F3A912897C070ADBF76221572C52609D
	echo "deb https://apt.dockerproject.org/repo ubuntu-trusty main" | sudo tee /etc/apt/sources.list.d/docker.list
	sudo apt-get update
	sudo apt-get install docker-engine
	```

14. Once docker is installed, we start our susi-server by entering:
	```
	sudo docker pull fossasia/susi_server
	sudo docker run -d -p 80:80 -p 443:443 fossasia/susi_server:latest
	```

15. Check if Susi server is running on your server, by going to your public DNS, e.g.: ```ec2-54-169-103-75.ap-southeast-1.compute.amazonaws.com```.
