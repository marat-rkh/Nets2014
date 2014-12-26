== Third  HW ==

# Protocol:

Client => Server

Client asks server to solve a bunch of quadric equations. He sends:

[number of equations | coeff 1 | coeff 2 | ... | coeff (number of equations * 3)]

Coefficients are random integers.

Server => Client

Server finds the number of solutions for each equation, adds this number to 500 and calculates the factorial (just for fun)
The result is: [result for equation 1 | ... | result for equation (num of equations)]

Experiments:

We fix some number of equations (the size of request). Then:

for clients_num in [10, 20, .. , 100]:

    for test_num in [1, 2, 3]:

        1) run clients_num clien ets, each one sends request to 
        server of the fixed size, mesures time of events and 
        store time data in separate file

        2) calculate average time characteristics per client 
        (sum and divide by clients_num)

    calculate average time characteristics per test (sum and divide by 3)

For outer loop we calculate the average time characteristics, for example the average response time per client.
Inner loop is used to make out experiment a little more precise.

All this process is repeated for 10, 50, 100, 200, 400 and 600 equations number requests.

Resuls:

Resulting graphs are in the folder Experiments