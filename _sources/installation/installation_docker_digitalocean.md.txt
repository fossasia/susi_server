# Installation of Susi Server with Docker on DigitalOcean

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
5. Pull Docker image from [Susi Server repository](https://hub.docker.com/r/fossasia/susi_server/) in Docker Hub (it should take about a minute):
   ```bash
   docker pull fossasia/susi_server
   ```

6. OK, you're ready to run Susi:
   ```bash
   docker run -d -p 80:80 -p 443:443 fossasia/susi_server:latest
   ```

7. Go to your droplet IP using web browser. You should see Susi-server main page.
