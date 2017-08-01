# Accounting in SUSI WebChat

SUSI WebChat provides the Users two options :
>- Chat Anonymously
>- Logged In

#### Chat Anonymously

The Chat Anonymously feature is for users to try out SUSI without any hassle of registering or logging in.
The users can interact with SUSI and try out its features like changing themes and searching messages but features like initializing the app with preferred defaults aren't provided in this mode.

#### Logged In

Users can register in the app and login to use SUSI with all its features.
Users can choose either the standard server or a personal server for SUSI.
>- Standard Server :  http://api.susi.ai/ 
>- Custom Server / Personal Server : URL of the users hosting of SUSI 

Users can modify and host their own version of [SUSI](https://github.com/fossasia/susi_server) and use that as their Personal Server.
Here is a [guide](https://github.com/fossasia/susi_server/tree/development/docs/installation) to hosting SUSI on various platforms

For all the accounting features, users can choose between standard server or their own personal server.

Let `BASE_URL` be Standard or Personal SUSI Servers URL.

##### Signup
The first step is registering with SUSI. User is asked for :
>- Email
>- Password
>- SUSI Server

Signup endpoint : `BASE_URL+'/aaa/signup.json?signup=EMAIL&password=PASSWORD;`

User is then sent a verification link to confirm the sign up. And upon verifying through mail, the sign up process is completed.

##### Login
 Once the user has registered with the server, he can directly login using :
 >- Email
 >- Password
>- SUSI Server
 
Login endpoint : `BASE_URL+'/aaa/login.json?type=access-token&login=EMAIL&password=PASSWORD';`

##### Password Recovery
Incase the user forgets his password, he can use the `Forgot Password` option to reset his password.
The Password recovery service uses :
>- Email
>- SUSI Server

Password Recovery endpoint : `BASE_URL+'/aaa/recoverpassword.json?forgotemail=EMAIL'`

A confirmation link is sent to the provided email with a link to `Reset Password` service.
Upon clicking that link, the user is redirected to a reset password service app where the user is asked for :
>- New Password

Reset password Redirect : `BASE_URL+'/apps/resetpass/index.html?token=TOKEN'`
'
##### Change Password
Incase the user wants to change his password, he can login using his current password and preferred server and use the `Change Password` option to set a new password for his account.
This service is different from `Password Recovery` service where the user has forgot his password and wants a new password. Here the user knows his current password and wants to change it.
The Change Password service uses:
>- Email
>- Current Password
>- New Password

Change Password endpoint : `BASE_URL+'/aaa/changepassword.json? 'changepassword=EMAIL&password=PASSWORD&newpassword=NEW_PASSWORD &access_token=ACCESS_TOKEN'`

Upon successfully changing password, the user is prompted to login again and is logged out automatically.

## Storing User Settings in Server

User can be using multiple chat clients and the state of the app must be maintained across all chat clients when the user is logged in and also within the same chat client upon login-logout. So every chat client must store user specific data in the server to ensure that all chat clients access this data and maintain the same state for that particular user and must accordingly push and pull user data from and to the server to update the state of the app.

The endpoint used to fetch User Settings is :
`BASE_URL+'/aaa/listUserSettings.json?access_token=ACCESS_TOKEN'`

The server returns a JSON object with the existing settings stored for that user
```
{
	"session": {
		"identity": {
			"type": ,
			"name": ,
			"anonymous": 
		}
	},
	"settings": {
		"SETTING_NAME": "SETTING_VALUE"
   }
}
```
The client fetches the settings upon login and initialises the app accordingly.

The endpoint used to add or update User Settings is :
`BASE_URL+'/aaa/changeUserSettings.json?key=SETTING_NAME&value=SETTING_VALUE&access_token=ACCESS_TOKEN'`

The server returns a JSON object with a message indicating if the settings were updated successfully or not.
```
{
	"session": {
		"identity": {
			"type": ,
			"name": ,
			"anonymous": 
		}
	},
	"message":
}
```
Whenever user settings are changed, the client updates the changed settings on the server so that the state is maintained across all chat clients

The current settings and notations implemented are :

- **Theme**:
      -- Used to change theme of the ChatApp.
      -- SETTING_NAME : `theme`
      -- SETTING_VALUE : `light/dark`

- **Enter As Send**:
      -- Used for multi line queries input.
      -- SETTING_NAME : `enter_send`
      -- SETTING_VALUE : `true/false`
      -- True means pressing enter will send message. False means pressing enter adds a new line.

- **Mic Input**
     -- Used to enable speech input.
     -- SETTING_NAME :  `mic_input`
     -- SETTING_VALUE : `true/false`
      -- True means default input method is speech but supports keyboard input too. False means the only input method shown is keyboard input.

- **Speech Output**
     -- Used to enable speech output when input type is speech
     -- SETTING_NAME :  `speech_output`
     -- SETTING_VALUE : `true/false`
      -- Upon speech input, True means we get a speech output & False means we don't get a speech output.

- **Speech Output Always ON**
     -- Used to enable speech output irrespective input type.
     -- SETTING_NAME :  `speech_always`
     -- SETTING_VALUE : `true/false`
      -- Upon speech/keyboard input, True means we get a speech output & False means we don't get a speech output unless ***Speech Output*** is set to ***true*** & the ***input type is speech***.
