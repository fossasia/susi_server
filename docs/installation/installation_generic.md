# Installation on Linux or Mac OS

The installation of the susi_server is easy but you must set up configurations for public and private skills as well.
Public skills are hosted in https://github.com/fossasia/susi_skill_data and private skills are hosted in a git repository which you must create yourself.

The final set-up could look like this:

```
.
├── susi_private_skill_data
├── susi_private_skill_data_host
├── susi_server
├── susi_skill_data
```

You may recognize that there is a fourth path, `susi_private_skill_data_host`. This is where the `susi_private_skill_data` is hosted.

## Requirements
- install git, jdk 8 and ant

## Download
- `git clone https://github.com/fossasia/susi_server.git`
- `cd susi_server`

## Build
- `ant` (just this, type "ant" - without quotes - and hit enter)

## Run
- `bin/start.sh`

## Operate
- open `http://localhost:4000` in your browser

## Shut down
- `bin/stop.sh`

