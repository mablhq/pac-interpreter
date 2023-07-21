// Source: https://github.com/manugarg/pactester/blob/master/pac_utils.js

/* This file is an adaption of netwerk/base/src/nsProxyAutoConfig.js file in
 * mozilla source code.
 *
 * **** BEGIN LICENSE BLOCK ****
 * Version: LGPL 2.1

 * This file is a free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.

 * This file is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.

 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301,
 * USA
 * **** END LICENSE BLOCK ****
 *
 * Original Contributors:
 *   Akhil Arora <akhil.arora@sun.com>
 *   Tomi Leppikangas <Tomi.Leppikangas@oulu.fi>
 *   Darin Fisher <darin@meer.net>
 *   Gagan Saksena 04/24/00
 *
 * Adapted for pactester by:
 *   Manu Garg <manugarg@google.com> 01/10/2007
 *
 * Adapted for GraalVM by:
 *   James Baldassari <james@mabl.com> 07/14/2023
 */

function dnsDomainIs(host, domain) {
    return (host.length >= domain.length &&
            host.substring(host.length - domain.length) == domain);
}
function dnsDomainLevels(host) {
    return host.split('.').length-1;
}
function convert_addr(ipchars) {
    var bytes = ipchars.split('.');
    var result = ((bytes[0] & 0xff) << 24) |
                 ((bytes[1] & 0xff) << 16) |
                 ((bytes[2] & 0xff) <<  8) |
                  (bytes[3] & 0xff);
    return result;
}
function isInNet(ipaddr, pattern, maskstr) {
    var test = new RegExp("^(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})$").exec(ipaddr);
    if (test == null) {
        ipaddr = dnsResolve(ipaddr);
        if (ipaddr == 'null')
            return false;
    } else if (test[1] > 255 || test[2] > 255 ||
               test[3] > 255 || test[4] > 255) {
        return false;    // not an IP address
    }
    var host = convert_addr(ipaddr);
    var pat  = convert_addr(pattern);
    var mask = convert_addr(maskstr);
    return ((host & mask) == (pat & mask));

}
function isPlainHostName(host) {
    return (host.search('\\.') == -1);
}
function isResolvable(host) {
    var ip = dnsResolve(host);
    return (ip != 'null');
}
function localHostOrDomainIs(host, hostdom) {
    return (host == hostdom) ||
           (hostdom.lastIndexOf(host + '.', 0) == 0);
}
function shExpMatch(url, pattern) {
   pattern = pattern.replace(/\./g, '\\.');
   pattern = pattern.replace(/\*/g, '.*');
   pattern = pattern.replace(/\?/g, '.');
   var newRe = new RegExp('^'+pattern+'$');
   return newRe.test(url);
}
var wdays = new Array('SUN', 'MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT');
var monthes = new Array('JAN', 'FEB', 'MAR', 'APR', 'MAY', 'JUN', 'JUL', 'AUG', 'SEP', 'OCT', 'NOV', 'DEC');
function weekdayRange() {
    function getDay(weekday) {
        for (var i = 0; i < 6; i++) {
            if (weekday == wdays[i])
                return i;
        }
        return -1;
    }
    var date = new Date();
    var argc = arguments.length;
    var wday;
    if (argc < 1)
        return false;
    if (arguments[argc - 1] == 'GMT') {
        argc--;
        wday = date.getUTCDay();
    } else {
        wday = date.getDay();
    }
    var wd1 = getDay(arguments[0]);
    var wd2 = (argc == 2) ? getDay(arguments[1]) : wd1;
    return (wd1 == -1 || wd2 == -1) ? false
                                    : (wd1 <= wday && wday <= wd2);
}
function dateRange() {
    function getMonth(name) {
        for (var i = 0; i < 6; i++) {
            if (name == monthes[i])
                return i;
        }
        return -1;
    }
    var date = new Date();
    var argc = arguments.length;
    if (argc < 1) {
        return false;
    }
    var isGMT = (arguments[argc - 1] == 'GMT');

    if (isGMT) {
        argc--;
    }
    // function will work even without explict handling of this case
    if (argc == 1) {
        var tmp = parseInt(arguments[0]);
        if (isNaN(tmp)) {
            return ((isGMT ? date.getUTCMonth() : date.getMonth()) ==
getMonth(arguments[0]));
        } else if (tmp < 32) {
            return ((isGMT ? date.getUTCDate() : date.getDate()) == tmp);
        } else {
            return ((isGMT ? date.getUTCFullYear() : date.getFullYear()) ==
tmp);
        }
    }
    var year = date.getFullYear();
    var date1, date2;
    date1 = new Date(year,  0,  1,  0,  0,  0);
    date2 = new Date(year, 11, 31, 23, 59, 59);
    var adjustMonth = false;
    for (var i = 0; i < (argc >> 1); i++) {
        var tmp = parseInt(arguments[i]);
        if (isNaN(tmp)) {
            var mon = getMonth(arguments[i]);
            date1.setMonth(mon);
        } else if (tmp < 32) {
            adjustMonth = (argc <= 2);
            date1.setDate(tmp);
        } else {
            date1.setFullYear(tmp);
        }
    }
    for (var i = (argc >> 1); i < argc; i++) {
        var tmp = parseInt(arguments[i]);
        if (isNaN(tmp)) {
            var mon = getMonth(arguments[i]);
            date2.setMonth(mon);
        } else if (tmp < 32) {
            date2.setDate(tmp);
        } else {
            date2.setFullYear(tmp);
        }
    }
    if (adjustMonth) {
        date1.setMonth(date.getMonth());
        date2.setMonth(date.getMonth());
    }
    if (isGMT) {
    var tmp = date;
        tmp.setFullYear(date.getUTCFullYear());
        tmp.setMonth(date.getUTCMonth());
        tmp.setDate(date.getUTCDate());
        tmp.setHours(date.getUTCHours());
        tmp.setMinutes(date.getUTCMinutes());
        tmp.setSeconds(date.getUTCSeconds());
        date = tmp;
    }
    return ((date1 <= date) && (date <= date2));
}
function timeRange() {
    var argc = arguments.length;
    var date = new Date();
    var isGMT= false;

    if (argc < 1) {
        return false;
    }
    if (arguments[argc - 1] == 'GMT') {
        isGMT = true;
        argc--;
    }

    var hour = isGMT ? date.getUTCHours() : date.getHours();
    var date1, date2;
    date1 = new Date();
    date2 = new Date();

    if (argc == 1) {
        return (hour == arguments[0]);
    } else if (argc == 2) {
        return ((arguments[0] <= hour) && (hour <= arguments[1]));
    } else {
        switch (argc) {
        case 6:
            date1.setSeconds(arguments[2]);
            date2.setSeconds(arguments[5]);
        case 4:
            var middle = argc >> 1;
            date1.setHours(arguments[0]);
            date1.setMinutes(arguments[1]);
            date2.setHours(arguments[middle]);
            date2.setMinutes(arguments[middle + 1]);
            if (middle == 2) {
                date2.setSeconds(59);
            }
            break;
        default:
          throw 'timeRange: bad number of arguments'
        }
    }

    if (isGMT) {
        date.setFullYear(date.getUTCFullYear());
        date.setMonth(date.getUTCMonth());
        date.setDate(date.getUTCDate());
        date.setHours(date.getUTCHours());
        date.setMinutes(date.getUTCMinutes());
        date.setSeconds(date.getUTCSeconds());
    }
    return ((date1 <= date) && (date <= date2));
}

// The following functions rely on GraalVM's Java integration because they require
// functionality that is not available in pure JavaScript (e.g. host => IP resolution):
function dnsResolve(host) {
    return Java.type('java.net.InetAddress')
        .getByName(host)
        .getHostAddress();
}
function myIpAddress() {
    return Java.type('java.net.InetAddress')
        .getLocalHost()
        .getHostAddress();
}
