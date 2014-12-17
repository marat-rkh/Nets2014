/*  Copyright (C) 2011-2013  P.D. Buchan (pdbuchan@yahoo.com)

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

// Send an IPv4 ICMP packet via raw socket.
// Stack fills out layer 2 (data link) information (MAC addresses) for us.
// Values set for echo request packet, includes some ICMP data.

#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>           // close()
#include <string.h>           // strcpy, memset(), and memcpy()

#include <netdb.h>            // struct addrinfo
#include <sys/types.h>        // needed for socket(), uint8_t, uint16_t, uint32_t
#include <sys/socket.h>       // needed for socket()
#include <netinet/in.h>       // IPPROTO_RAW, IPPROTO_IP, IPPROTO_ICMP, INET_ADDRSTRLEN
#include <netinet/ip.h>       // struct ip and IP_MAXPACKET (which is 65535)
#include <netinet/ip_icmp.h>  // struct icmp, ICMP_ECHO
#include <arpa/inet.h>        // inet_pton() and inet_ntop()
#include <sys/ioctl.h>        // macro ioctl is defined
#include <bits/ioctls.h>      // defines values for argument "request" of ioctl.
#include <net/if.h>           // struct ifreq

#include <errno.h>            // errno, perror()

// Define some constants.
const uint16_t IP4_HDRLEN = 20;         // IPv4 header length
const uint16_t ICMP_HDRLEN = 8;         // ICMP header length for echo request, excludes data
const uint16_t ICMP_BODY_LEN = 12;

// Function prototypes
uint16_t checksum (uint16_t *, int);
char *allocate_strmem (int);
uint8_t *allocate_ustrmem (int);
int *allocate_intmem (int);

ifreq get_interface_info() {
    // Interface to send packet through.
    char* interface = allocate_strmem (40);
    strcpy (interface, "wlan0");

    // Submit request for a socket descriptor to look up interface.
    int sd;
    if ((sd = socket (AF_INET, SOCK_RAW, IPPROTO_RAW)) < 0) {
        perror ("socket() failed to get socket descriptor for using ioctl() ");
        exit (EXIT_FAILURE);
    }

    // Use ioctl() to look up interface index which we will use to
    // bind socket descriptor sd to specified interface with setsockopt() since
    // none of the other arguments of sendto() specify which interface to use.
    struct ifreq ifr;
    memset (&ifr, 0, sizeof (ifr));
    snprintf (ifr.ifr_name, sizeof (ifr.ifr_name), "%s", interface);
    if (ioctl (sd, SIOCGIFINDEX, &ifr) < 0) {
        perror ("ioctl() failed to find interface ");
        exit (EXIT_FAILURE);
    }
    close (sd);
    free(interface);
    printf ("Index for interface %s is %i\n", interface, ifr.ifr_ifindex);
    return ifr;
}

char* alloc_dst_binary(char const* dst_str) {
    // Destination URL or IPv4 address: you need to fill this out
    char* target = allocate_strmem (40);
    strcpy (target, dst_str);

    // Fill out hints for getaddrinfo().
    struct addrinfo hints;
    memset (&hints, 0, sizeof (struct addrinfo));
    hints.ai_family = AF_INET;
    hints.ai_socktype = SOCK_STREAM;
    hints.ai_flags = hints.ai_flags | AI_CANONNAME;

    // Resolve target using getaddrinfo().
    int status;
    struct addrinfo* res;
    if ((status = getaddrinfo (target, NULL, &hints, &res)) != 0) {
        fprintf (stderr, "getaddrinfo() failed: %s\n", gai_strerror (status));
        exit (EXIT_FAILURE);
    }
    struct sockaddr_in *ipv4 = (struct sockaddr_in *) res->ai_addr;
    void* tmp = &(ipv4->sin_addr);
    char* dst_ip = allocate_strmem (INET_ADDRSTRLEN);
    if (inet_ntop (AF_INET, tmp, dst_ip, INET_ADDRSTRLEN) == NULL) {
        status = errno;
        fprintf (stderr, "inet_ntop() failed.\nError message: %s", strerror (status));
        exit (EXIT_FAILURE);
    }
    freeaddrinfo (res);
    free (target);
    return dst_ip;
}

ip create_ip_header(char* src_ip, char* dst_ip, int datalen) {
    // IPv4 header
    struct ip iphdr;
    // IPv4 header length (4 bits): Number of 32-bit words in header = 5
    iphdr.ip_hl = IP4_HDRLEN / sizeof (uint32_t);
    // Internet Protocol version (4 bits): IPv4
    iphdr.ip_v = 4;
    // Type of service (8 bits)
    iphdr.ip_tos = 0;
    // Total length of datagram (16 bits): IP header + ICMP header + ICMP data
    iphdr.ip_len = htons (IP4_HDRLEN + ICMP_HDRLEN + datalen);
    // ID sequence number (16 bits): unused, since single datagram
    iphdr.ip_id = htons (0);

    // Flags, and Fragmentation offset (3, 13 bits): 0 since single datagram
    int* ip_flags = allocate_intmem (4);
    // Zero (1 bit)
    ip_flags[0] = 0;
    // Do not fragment flag (1 bit)
    ip_flags[1] = 0;
    // More fragments following flag (1 bit)
    ip_flags[2] = 0;
    // Fragmentation offset (13 bits)
    ip_flags[3] = 0;
    iphdr.ip_off = htons ((ip_flags[0] << 15)
            + (ip_flags[1] << 14)
            + (ip_flags[2] << 13)
            +  ip_flags[3]);

    // Time-to-Live (8 bits): default to maximum value
    iphdr.ip_ttl = 255;
    // Transport layer protocol (8 bits): 1 for ICMP
    iphdr.ip_p = IPPROTO_ICMP;

    // Source IPv4 address (32 bits)
    int status;
    if ((status = inet_pton (AF_INET, src_ip, &(iphdr.ip_src))) != 1) {
        fprintf (stderr, "inet_pton() failed.\nError message: %s", strerror (status));
        exit (EXIT_FAILURE);
    }
    // Destination IPv4 address (32 bits)
    if ((status = inet_pton (AF_INET, dst_ip, &(iphdr.ip_dst))) != 1) {
        fprintf (stderr, "inet_pton() failed.\nError message: %s", strerror (status));
        exit (EXIT_FAILURE);
    }

    // IPv4 header checksum (16 bits): set to 0 when calculating checksum
    iphdr.ip_sum = 0;
    iphdr.ip_sum = checksum ((uint16_t *) &iphdr, IP4_HDRLEN);

    free (ip_flags);
    return iphdr;
}

icmp create_icmp_header() {
    // ICMP header
    struct icmp icmphdr;
    // Message Type (8 bits)
    icmphdr.icmp_type = ICMP_ECHO;
    // Message Code (8 bits)
    icmphdr.icmp_code = 0;
    // Identifier (16 bits): usually pid of sending process - pick a number
    icmphdr.icmp_id = htons (1000);
    // Sequence Number (16 bits): starts at 0
    icmphdr.icmp_seq = htons (0);
    // ICMP header checksum (16 bits): set to 0 when calculating checksum
    icmphdr.icmp_cksum = 0;
    return icmphdr;
}

int create_socket(ifreq& ifr) {
    int sd;
    if ((sd = socket (AF_INET, SOCK_RAW, IPPROTO_RAW)) < 0) {
        perror ("socket() failed ");
        exit (EXIT_FAILURE);
    }

    const int on = 1;
    // Set flag so socket expects us to provide IPv4 header.
    if (setsockopt (sd, IPPROTO_IP, IP_HDRINCL, &on, sizeof (on)) < 0) {
        perror ("setsockopt() failed to set IP_HDRINCL ");
        exit (EXIT_FAILURE);
    }

    // Bind socket to interface index.
    if (setsockopt (sd, SOL_SOCKET, SO_BINDTODEVICE, &ifr, sizeof (ifr)) < 0) {
        perror ("setsockopt() failed to bind to interface ");
        exit (EXIT_FAILURE);
    }
    return sd;
}

int main () {
    struct ifreq ifr = get_interface_info();

    // Source IPv4 address: you need to fill this out
    char* src_ip = allocate_strmem (INET_ADDRSTRLEN);
    strcpy (src_ip, "192.168.1.132");

    // Destination URL or IPv4 address: you need to fill this out
    char* dst_ip = alloc_dst_binary("github.com");

    // IPv4 header
    int datalen = 4;
    struct ip iphdr = create_ip_header(src_ip, dst_ip, datalen);
    free(src_ip);
    free(dst_ip);

    // ICMP header
    struct icmp icmphdr = create_icmp_header();

    // ICMP data
    uint8_t *data = allocate_ustrmem (IP_MAXPACKET);
    data[0] = 'T';
    data[1] = 'e';
    data[2] = 's';
    data[3] = 't';

    // Prepare packet.
    uint8_t *packet = allocate_ustrmem (IP_MAXPACKET);
    // First part is an IPv4 header.
    memcpy (packet, &iphdr, IP4_HDRLEN);
    // Next part of packet is upper layer protocol header.
    memcpy ((packet + IP4_HDRLEN), &icmphdr, ICMP_HDRLEN);
    // Finally, add the ICMP data.
    memcpy (packet + IP4_HDRLEN + ICMP_HDRLEN, data, datalen);
    // Calculate ICMP header checksum
    icmphdr.icmp_cksum = checksum ((uint16_t *) (packet + IP4_HDRLEN), ICMP_HDRLEN + datalen);
    memcpy ((packet + IP4_HDRLEN), &icmphdr, ICMP_HDRLEN);

    // The kernel is going to prepare layer 2 information (ethernet frame header) for us.
    // For that, we need to specify a destination for the kernel in order for it
    // to decide where to send the raw datagram. We fill in a struct in_addr with
    // the desired destination IP address, and pass this structure to the sendto() function.
    struct sockaddr_in sin;
    memset (&sin, 0, sizeof (struct sockaddr_in));
    sin.sin_family = AF_INET;
    sin.sin_addr.s_addr = iphdr.ip_dst.s_addr;

    // Submit request for a raw socket descriptor.
    int sd = create_socket(ifr);

    // Send packet.
    for(size_t i = 0; i != 100; ++i) {
        if (sendto (sd, packet, IP4_HDRLEN + ICMP_HDRLEN + datalen, 0,
                    (struct sockaddr *) &sin, sizeof (struct sockaddr)) < 0)  {
            perror ("sendto() failed ");
            exit (EXIT_FAILURE);
        }
    }

    // Close socket descriptor.
    close (sd);
    // Free allocated memory.
    free (data);
    free (packet);

    return (EXIT_SUCCESS);
}

// Checksum function
uint16_t checksum (uint16_t *addr, int len) {
    int nleft = len;
    int sum = 0;
    uint16_t *w = addr;
    uint16_t answer = 0;

    while (nleft > 1) {
        sum += *w++;
        nleft -= sizeof (uint16_t);
    }

    if (nleft == 1) {
        *(uint8_t *) (&answer) = *(uint8_t *) w;
        sum += answer;
    }

    sum = (sum >> 16) + (sum & 0xFFFF);
    sum += (sum >> 16);
    answer = ~sum;
    return (answer);
}

// Allocate memory for an array of chars.
char* allocate_strmem (int len)
{
    char *tmp;

    if (len <= 0) {
        fprintf (stderr, "ERROR: Cannot allocate memory because len = %i in allocate_strmem().\n", len);
        exit (EXIT_FAILURE);
    }

    tmp = (char *) malloc (len * sizeof (char));
    if (tmp != NULL) {
        memset (tmp, 0, len * sizeof (char));
        return (tmp);
    } else {
        fprintf (stderr, "ERROR: Cannot allocate memory for array allocate_strmem().\n");
        exit (EXIT_FAILURE);
    }
}

// Allocate memory for an array of unsigned chars.
uint8_t * allocate_ustrmem (int len) {
    u_int8_t *tmp;

    if (len <= 0) {
        fprintf (stderr, "ERROR: Cannot allocate memory because len = %i in allocate_ustrmem().\n", len);
        exit (EXIT_FAILURE);
    }

    tmp = (uint8_t *) malloc (len * sizeof (uint8_t));
    if (tmp != NULL) {
        memset (tmp, 0, len * sizeof (uint8_t));
        return (tmp);
    } else {
        fprintf (stderr, "ERROR: Cannot allocate memory for array allocate_ustrmem().\n");
        exit (EXIT_FAILURE);
    }
}

// Allocate memory for an array of ints.
int* allocate_intmem (int len) {
    int *tmp;

    if (len <= 0) {
        fprintf (stderr, "ERROR: Cannot allocate memory because len = %i in allocate_intmem().\n", len);
        exit (EXIT_FAILURE);
    }

    tmp = (int *) malloc (len * sizeof (int));
    if (tmp != NULL) {
        memset (tmp, 0, len * sizeof (int));
        return (tmp);
    } else {
        fprintf (stderr, "ERROR: Cannot allocate memory for array allocate_intmem().\n");
        exit (EXIT_FAILURE);
    }
}
