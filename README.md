Information:
	Implementation involves 3 parts.
		Data Extractor:
			Take the ~5 million movie titles from imdb and store them into a text file movies.txt. Then, ReadFiles.scala parses the ~5 million movies and takes a sub-section of the list, storing it into a list of movies and their computed hashed values. Some moves may hash to the same value- this side effect was anticipated and worked with.
		Users:
			One of the inputs to the simulation is the number of users in the simulation, n. Then, an array of users (size n) is created. Each user is in fact an actor that is given a specific role, either READ or WRITE, depending on the ratio specified in the input to the simulator, and each actors job is to perform its designated times no more than allowed in the input. The array of users is stored and managed by a UserManager actorRef. It is responsible for initializing and sending messages to the userActors. The actors themselves send and receive messages to and from other actors in the server as well as the actors in the nodes server.
			
			If the assigned task of the user is READ, then it performs a GET request to the server, passing in a KEY, and waiting for a VALUE response from one of the node actors in the server.
			
			If the assigned task of the user is WRITE, then it performs a PUT request to the server, passing in a KEY and VALUE, and waiting for a confirmation response from the server.

		Servers:
			As per the spec, there are m nodes (specified by the user) that are created in the server ring according to the chord protocol. These nodes listen to requests given by the users. The requests these nodes listen for are READ and WRITE, and act in the following way depending on the request:
			
				READ:
					On a read request sent to a node in the ring, the node will send back to the calling actor a KEY-VALUE pair containing the hashed value and the name of the movie that matches the key. If the movie name and hashed value is not found, "Not found" is returned to the asker via a message.
				
				WRITE:
					On a write request sent to a node in the ring, the node will analyze the hash value passed to it, and based on that value evaluates where the VALUE of the KEY-VALUE pair should be stored. If the hash value falls out of the range of the actors designated range, it sends this KEY-VALUE pair onto the next actor, who performs the same evaluation, until a node is found in which the KEY-VALUE pair can be stored (to be available for a future read request). Determination of the which node to pass the key/value pair onto is determined by the fingertable that determines which nodes in the ring have the proper hash range that matches the key. 
					
			In the event that a node drops from the ring, all of its pointers and ranges are transferred over to a neighbor node, and the finger table references are re-balanced so that those KEY-VALUE pairs can still be searched for by any user actor.
			
Structure:
		Entry Point: (Data taken in from the user)
			-> Initializes Actors in the ClientManager actor
			-> Initializes Actors in the ServerManager actor
			-> Starts a simulation timer
			-> Starts a cycle of requests by the actors
		
		ClientManager: (Manage messages to and from user actors)
			-> handlers for the various messages to user actors
			
			User: (Individual user with its own assigned task)
				-> Simply performs one action for its entire lifetime(READ or WRITE) to the server. This can happen only a certain number of times per minunte.
			
		ServerManager: (Manage messages to and from node actors in the server)
			-> handlers for the various messages to node actors
		
			Node: (Individual node as part of the ring)
				-> Passes around read requests to neighbor nodes if key value pair is not found in self
				-> Stores KEY-VALUE pairs from PUT requests into a local list of kv-pairs
			
How to run:
	1. Build sbt to resolve any dependencies
	2. Run EntryPoint.scala
		- User will be prompted for the various inputs
		(NOTE: no error handling implemented: if asked for a decimal, enter a decimal, not whole number, etc)
		
		Here is some sample inputs that can be used:
			- Enter the duration of the simulation (in minutes) : 100
			- Enter the # of users : 10000
			- Enter the MINIMUM number of requests each user actor can send every minute : 10
			- Enter the MAXIMUM number of requests each user actor can send every minute : 100
			- Enter the # of computers : 100
			- Enter the name of the file that contains Key/Value pairs : input.txt
			- Enter the ratio of Write/Read requests <= 1.0 (as DECIMAL) : .8
			- Enter the time mark (elapsed time in SECONDS) of when to pause the system and dump state as an XML : 60
			
	3. (OPTIONAL) View log.log in the main project directory


Tests:
	5 tests can be found and run in ./test, in the ./src directory.
		- Simple Test
			+ This was simply to test whether the testing framework is working properly, and that we can instantiate/and run a test.
		- Reading From File Input 
			+ This test tests the reading from a file method defined inside the EntryPoint object which servers as the starting point of the simulation
		- Writing From File Input
			+ This test tests the writing from a file method defined inside the EntryPoint object which servers as the starting point of the simulation
		- NodesServer Instantiation
			+ This test tests' the futures when instantiating the NodesServer. The test waits for the server to instantiate and asserts it's success
		- ClientServer Instantiation
			+ This test tests' the futures when instantiating the ClientServer. The test waits for the server to instantiate and asserts it's success
