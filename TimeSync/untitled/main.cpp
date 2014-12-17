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

#include <sys/time.h>
#include <time.h>

// Define some constants.
const uint16_t IP4_HDRLEN = 20;         // IPv4 header length
const uint16_t ICMP_HDRLEN = 8;         // ICMP header length for echo request, excludes data
const uint16_t ICMP_BODY_LEN = 12;
const uint16_t FULL_LEN = IP4_HDRLEN + ICMP_HDRLEN + ICMP_BODY_LEN;

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

ip create_ip_header(char* src_ip, char* dst_ip) {
    struct ip iphdr;
    // IPv4 header length (4 bits): Number of 32-bit words in header = 5
    iphdr.ip_hl = IP4_HDRLEN / sizeof (uint32_t);
    iphdr.ip_v = 4;
    iphdr.ip_tos = 0;
    iphdr.ip_len = htons (IP4_HDRLEN + ICMP_HDRLEN + ICMP_BODY_LEN);
    iphdr.ip_id = htons (0);

    // Flags, and Fragmentation offset (3, 13 bits): 0 since single datagram
    int* ip_flags = allocate_intmem (4);
    ip_flags[0] = 0; // reserved
    ip_flags[1] = 0; // Do not fragment flag (1 bit)
    ip_flags[2] = 0; // More fragments following flag (1 bit)
    ip_flags[3] = 0; // Fragmentation offset (13 bits)
    iphdr.ip_off = htons ((ip_flags[0] << 15)
            + (ip_flags[1] << 14)
            + (ip_flags[2] << 13)
            +  ip_flags[3]);
    iphdr.ip_ttl = 255;
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
    iphdr.ip_sum = checksum ((uint16_t *) &iphdr, IP4_HDRLEN);

    free(ip_flags);
    return iphdr;
}

icmp create_icmp_header() {
    struct icmp icmphdr;
    icmphdr.icmp_type = ICMP_TIMESTAMP;
    icmphdr.icmp_code = 0;
    icmphdr.icmp_id = htons (1000); // randomly selected
    icmphdr.icmp_seq = htons (0);
    icmphdr.icmp_cksum = 0; // will be set further
    return icmphdr;
}

int create_send_socket(ifreq& ifr) {
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

uint32_t bytes_to_millisec(uint8_t* bytes, uint16_t start_from) {
    uint32_t time_in_ms = 0;
    char* time_as_bytes = (char *) &time_in_ms;
    for(uint16_t i = start_from; i != start_from + 4; ++i) {
        time_as_bytes[i - start_from] = bytes[i];
    }
    return time_in_ms;
}

uint32_t current_time() {
    timeval tv;
    if(gettimeofday(&tv, NULL) != 0) {
        perror("gettimeofday() failed");
        exit(EXIT_FAILURE);
    }
    return ((tv.tv_sec % 86400) * 1000 + tv.tv_usec / 1000);
}

void print_ms_time(uint32_t time_ms) {
    uint32_t ms = (uint32_t) time_ms % 1000;
    uint32_t sec = (uint32_t) (time_ms / 1000) % 60 ;
    uint32_t min = (uint32_t) ((time_ms / (1000*60)) % 60);
    uint32_t hr   = (uint32_t) ((time_ms / (1000*60*60)) % 24);
    printf("%d hr %d min %d sec %d ms", hr, min, sec, ms);
}

void sync_time(uint8_t* data) {
    uint32_t send_time = ntohl(bytes_to_millisec(data, 0));
    uint32_t remote_receive_time = ntohl(bytes_to_millisec(data, 4));
    uint32_t remote_send_time = ntohl(bytes_to_millisec(data, 8));
    uint32_t cur_remote_time = remote_send_time + (remote_receive_time - send_time);
    printf("Remote time is ");
    print_ms_time(cur_remote_time);
    printf("\nLocal  time is ");
    print_ms_time(current_time());
    printf("\n");
}

int main () {
    struct ifreq ifr = get_interface_info();

    // Source IPv4 address
    char* src_ip = allocate_strmem (INET_ADDRSTRLEN);
    strcpy (src_ip, "192.168.1.44");

    // Destination URL or IPv4 address
    char* dst_ip = alloc_dst_binary("github.com");

    // IPv4 header
    struct ip iphdr = create_ip_header(src_ip, dst_ip);
    free(src_ip);
    free(dst_ip);

    // ICMP header
    struct icmp icmphdr = create_icmp_header();

    // ICMP data
    uint8_t *data = allocate_ustrmem (IP_MAXPACKET);
    uint32_t cur_time_ms = htonl(current_time());
    memcpy(data, (char *) &cur_time_ms, 4);

    // Prepare packet.
    uint8_t *packet = allocate_ustrmem (IP_MAXPACKET);
    memcpy (packet, &iphdr, IP4_HDRLEN);
    memcpy (packet + IP4_HDRLEN, &icmphdr, ICMP_HDRLEN);
    memcpy (packet + IP4_HDRLEN + ICMP_HDRLEN, data, ICMP_BODY_LEN);
    icmphdr.icmp_cksum = checksum((uint16_t *) (packet + IP4_HDRLEN), ICMP_HDRLEN + ICMP_BODY_LEN);
    memcpy ((packet + IP4_HDRLEN), &icmphdr, ICMP_HDRLEN);

    // The kernel is going to prepare layer 2 information (ethernet frame header) for us.
    // For that, we need to specify a destination for the kernel in order for it
    // to decide where to send the raw datagram. We fill in a struct in_addr with
    // the desired destination IP address, and pass this structure to the sendto() function.
    struct sockaddr_in sin;
    memset (&sin, 0, sizeof (struct sockaddr_in));
    sin.sin_family = AF_INET;
    sin.sin_addr.s_addr = iphdr.ip_dst.s_addr;

    int send_sd = create_send_socket(ifr);

    // Create receive socket
    int recv_sd;
    if ((recv_sd = socket(AF_INET, SOCK_RAW, IPPROTO_ICMP)) < 0) {
        printf("Could not process socket() [recv].\n");
        return EXIT_FAILURE;
    }
    if(sendto(send_sd, packet, FULL_LEN, 0, (sockaddr *) &sin, sizeof (sockaddr)) < 0) {
        perror ("sendto() failed");
        exit (EXIT_FAILURE);
    }
    int status;
    if((status = recvfrom(recv_sd, data, IP_MAXPACKET, 0, 0, 0)) == -1) {
        perror("receive failed");
        exit(EXIT_FAILURE);
    }
    sync_time(data + IP4_HDRLEN + ICMP_HDRLEN);

    close(send_sd);
    close(recv_sd);
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
