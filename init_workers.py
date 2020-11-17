import subprocess
import json
import time


def writeAwsRunInstFile(processNames: str, fileName: str):
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
		start_processes = ""
		for p_name in process_names:
			start_processes += '\"python3 -m da -n ' + p_name + ' -D main.da\" '
		bashOnRun = "\n".join(["#!/bin/bash",
			"sudo yum update -y",
			"sudo yum -y install python37 -y",
			"sudo python3 -m pip install numpy", 
			"sudo yum -y install git -y", 
			"sudo yum install java-1.8.0-openjdk -y",
			"sudo yum install java-1.8.0-openjdk-devel -y",
			"cd /home/ec2-user", 
			"git clone https://github.com/thangbui1294/distrib-dtrmu.git", 
			"sudo chmod 777 /home/ec2-user/distrib-dtrmu/run-node.bash", 
			"cd /home/ec2-user/distrib-dtrmu/mining-algorithms/learning-data-generator",
			"cd src",
			"javac -cp \"../libs/commons-lang3-3.4.jar\" -d ../bin ./*.java",
			"cd ../../improve-policy",
			"cd src",
			"javac -cp \"../libs/commons-math-2.2.jar:../libs/commons-lang3-3.4.jar:../libs/commons-math3-3.3.jar:../libs/jdi.jar;\" -d ../bin algo/*.java util/*.java",
			"cd /home/ec2-user/distrib-dtrmu",
			"sudo chmod -R 777 run-exp",
			"sudo chmod -R 777 mining-algorithms",
			#"sh /home/ec2-user/distrib-dtrmu/run-workers.bash {}".format(start_processes)
			])
	
		f.write(headPart + bashOnRun)

def parseNewInstFeedbackToIp(outputMsg: str):
    jsonObj = json.loads(outputMsg)
    networkPart = jsonObj["Instances"][0]
    ipAddr = networkPart["NetworkInterfaces"][0]["PrivateIpAddresses"][0]["PrivateIpAddress"]
    return ipAddr

		
def create_worker_machines(num_vms, num_workers_per_vm):
	AWS_IMAGE_ID = "ami-0947d2ba12ee1ff75" 
	
	for i in range(num_vms):
		bashFileName = f"./inst-bash/VM-{i}.bash"
		process_names = []
		for j in range(num_workers_per_vm):
			node_id = i * len(num_workers_per_vm) + j + 1
			process_names.append(f"Worker{node_id}")
		writeAwsRunInstFile(process_names, bashFileName)

		# bashCommand = f"chmod 777 {bashFileName}"
		# process = subprocess.Popen(bashCommand.split(), stdout=subprocess.PIPE)
		# output, error = process.communicate()

		bashCommand = " ".join([
			"aws ec2 run-instances", 
			f"--image-id {AWS_IMAGE_ID}",
			"--count 1",
			"--instance-type t2.large",
			"--key-name thang_vm",
			"--subnet-id subnet-11a9da1f",
			"--security-group-ids sg-04347234995eecedf",
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
		daAddrs = []
		for j in range(num_workers_per_vm):
			node_id = i * len(num_workers_per_vm) + j + 1
			node_name = f"Worker{node_id}"
			daAddrs.append(f"{nodeName}@{ipAddr}")
		print(daAddrs)

		with open("./newDaAddr.config", "a") as f:
			for daAddr in daAddrs:
				f.write(daAddr + "\n")
	waitingTime = 150
	print(f"Waiting {waitingTime} seconds for the worker machines initialization")
	time.sleep(waitingTime)