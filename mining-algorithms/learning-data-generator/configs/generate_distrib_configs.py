__author__ = 'Thang'

for i in range(4):
    for j in range(5):
        with open('./eWorkforce_' + str(i) + '.conf', 'r') as f_ori:
            with open('./eWorkforce_' + str(i) + '_' + str(j) + '.conf', 'w') as f_out:
                for line in f_ori:
                    line = line.strip()
                    if 'numPoliciesPerSize=' in line:
                        f_out.write('numPoliciesPerSize=1\nrunPolicy=' + str(j) + '\n')
                    elif 'outputFile=' in line:
                        f_out.write('outputFile=../../output/scaling_f_3/output_eWorkforce_10_' + str(j) + '.txt\n')
                    else:
                        f_out.write(line + '\n')

