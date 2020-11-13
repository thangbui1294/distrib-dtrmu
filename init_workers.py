import subprocess
import json
import time


def writeAwsRunInstFile(nodeName: str, fileName: str):
	with open(fileName, 'w') as f:
		headPart = """Content-Type: multipart/mixed; boundary="//"
MIME-Version: 1.0

--//
Content-Type: text/cloud-config; charset="us-ascii"
MIME-Version: 1.0
Content-Transfer-Encoding: 7bit
Content-Disposition: attachment; filename="cloud-config.txt"

#cloud-config
cloud_final_modules:
- [scripts-user, always]

--//
Content-Type: text/x-shellscript; charset="us-ascii"
MIME-Version: 1.0
Content-Transfer-Encoding: 7bit
Content-Disposition: attachment; filename="userdata.txt"
\n"""
		
		bashOnRun = "\n".join(["#!/bin/bash",
			"sudo yum update -y",
			"sudo yum -y install python37 -y", 
			"sudo yum -y install git -y", 
			"sudo yum install java-1.8.0-openjdk -y",
			"sudo yum install java-1.8.0-openjdk-devel -y",
			"cd /home/ec2-user", 
			"git clone https://github.com/thangbui1294/distrib-dtrmu.git", 
			"sudo chmod 777 /home/ec2-user/distrib-dtrmu/run-node.bash", 
			"cd distrib-dtrmu/mining-algorithms/learning-data-generator",
			"cd src",
			"javac -cp \"../libs/commons-lang3-3.4.jar.\" -d ../bin ./*.java",
			"cd ../../improve-policy",
			"cd src",
			"javac -cp \"../libs/commons-math-2.2.jar;../libs/commons-lang3-3.4.jar;../libs/commons-math3-3.3.jar;../libs/jdi.jar.\" -d ../bin algo/*.java util/*.java",
			"cd /home/ec2-user/distrib-dtrmu",
			"sudo chmod -R 777 run-exp"
			#"sh /home/ec2-user/distrib-dtrmu/run-node.bash {}".format(nodeName)
			])
	
		f.write(headPart + bashOnRun)

def parseNewInstFeedbackToIp(outputMsg: str):
    jsonObj = json.loads(outputMsg)
    networkPart = jsonObj["Instances"][0]
    ipAddr = networkPart["NetworkInterfaces"][0]["PrivateIpAddresses"][0]["PrivateIpAddress"]
    return ipAddr

		
def create_worker_machines(num_workers):
	AWS_IMAGE_ID = "ami-0947d2ba12ee1ff75" 
	
	for i in range(1, num_workers + 1):
		bashFileName = f"./inst-bash/Worker-{i}.bash"
		nodeName = f"Worker{i}"
		writeAwsRunInstFile(nodeName, bashFileName)

		# bashCommand = f"chmod 777 {bashFileName}"
		# process = subprocess.Popen(bashCommand.split(), stdout=subprocess.PIPE)
		# output, error = process.communicate()

		bashCommand = " ".join([
			"aws ec2 run-instances", 
			f"--image-id {AWS_IMAGE_ID}",
			"--count 1",
			"--instance-type t2.micro",
			"--key-name thang_vm",
			"--subnet-id subnet-1107d858",
			"--security-group-ids sg-01dd4fd8b84bd3c1f",
			"--region us-east-1",
			# "--tag-specifications 'ResourceType=instance,Tags=[{Key=Name,Value=" + nodeName + "},{Key=type,Value=BatchGen}]'", 
			# "--tag-specifications 'ResourceType=instance,Tags=[{Key=Name,Value=" + nodeName + "}]'", 
			f"--user-data file://{bashFileName}"
			])
		# print(bashCommand)
		process = subprocess.Popen(bashCommand.split(), stdout=subprocess.PIPE)
		outputMsg, errorMsg = process.communicate()		
		# assert errorMsg == None
		if errorMsg is not None:
			print(errorMsg)

		ipAddr = parseNewInstFeedbackToIp(outputMsg)
		daAddr = f"{nodeName}@{ipAddr}"
		print(daAddr)

		with open("./newDaAddr.config", "a") as f:
			f.write(daAddr + "\n")
	waitingTime = 120
	print(f"Waiting {waitingTime} seconds for the worker machines initialization")
	time.sleep(waitingTime)