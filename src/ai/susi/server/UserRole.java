/**
 *  UserRole
 *  Copyright 17.05.2016 by Michael Peter Christen, @0rb1t3r
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *  
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *  
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program in the file lgpl21.txt
 *  If not, see <http://www.gnu.org/licenses/>.
 */


package ai.susi.server;

/**
 * A UserRole defines the servlet access right.
 * This categories are inspired by the wikipedia User Access Levels, see https://en.wikipedia.org/wiki/Wikipedia:User_access_levels
 * To assign a right manually, open the file data/setting/authorization.json
 * Change the "userRole" object to a lowercase version of one of these names.
 */
public enum UserRole {

    BOT,             // a technical access
    ANONYMOUS,       // a person, everyone who is not logged in
    USER,            // users who have logged in
    REVIEWER,        // users with special rights for content creation, i.e. moderators
    ACCOUNTCREATOR,  // users with special rights for user account creation
    ADMIN,           // a sysop which can assign accountcreator rights and can assign single access rights to any user. also: delete and restore pages, block and unblock users
    BUREAUCRAT;      // maximum right, that user is allowed to do everything
    
    public String getName() {
    	return this.name().toLowerCase();
    }
}
