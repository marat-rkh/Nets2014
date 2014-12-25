from subprocess import Popen

CLIENT_JAR_PATH = "../Client/out/artifacts/Client_jar/Client.jar"

# client program args
SERVER_IP = "192.168.1.3"
SERVER_PORT = "45213"
EQUATIONS_NUM = 10

#runner options
TESTS_NUM = 2
CLIENTS_NUM = 10

def run(tests_num, clients_num, equations_num):
    for i in xrange(0, tests_num):
        print "test " + str(i)
        processes = []
        for j in xrange(0, clients_num):
            proc = Popen(["java", "-jar", CLIENT_JAR_PATH, SERVER_IP, SERVER_PORT, str(equations_num)])
            processes.append(proc)
        for proc in processes:
            proc.communicate()
            

# main
run(TESTS_NUM, CLIENTS_NUM, EQUATIONS_NUM)