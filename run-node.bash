#!/bin/bash
export PYTHONPATH="/home/ec2-user/distrib-dtrmu/distalgo-master":${PYTHONPATH}
rm -f ~/main.da.log
chmod 777 /home/ec2-user/distrib-dtrmu/main.da
python3 -m da -n $1 -D /home/ec2-user/distrib-dtrmu/main.da
