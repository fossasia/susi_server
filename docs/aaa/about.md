AAA system
==================

**AAA** stands for: _Authentication_, _Authorization_, _Accounting_

* **Authentication**: recognize the user and get the account details
* **Authorization**: identify the access rights of the user and know what the user is allowed to do
* **Accounting**: remember what the user has done. I.e. here in Susi: remember the chat log.
For Authentication we have right now email addresses as main ID or anonymous accounts with IP numbers. We could also add twitter/facebook/instagram accounts as alternative way to authenticate. Lets not do that now. Just know that it could be possible in the future.

## Requiring a certain `UserRole`
The basic idea is, that the `AbstractAPIHandler` checks the permissions of the user using the roles of the user and comparing it with the value as defined in the method `getMinimalBaseUserRole()` of each `servlet` which extends `AbstractAPIHandler`.
Then, checks will happen automatically. Implementers of a servlet do not need to care about user permission checks, it should all be done with the declaration within `getMinimalBaseUserRole()`.
So for requiring an _ADMIN_ user role, just write 
```java
public BaseUserRole getMinimalBaseUserRole() {
 return BaseUserRole.ADMIN; 
}
```