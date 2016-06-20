/**
 *  APIServiceLevel
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


package org.loklak.server;

public enum APIServiceLevel {

    ADMIN,				// is allowed to do everything (currently: localhost access), including assignment of rights
    MODERATOR,			// has rights to use all services and to assign user rights, up to moderation rights
    SERVICE_MANAGER,	// has right to use all services and change service level settings, also for specific users
    USER,				// has rights as assigned
    WHITELIST_ONLY,		// has rights like ANONYMOUS, but can be assigned individual rights
    ANONYMOUS			// has lowest rights, public
    
}
