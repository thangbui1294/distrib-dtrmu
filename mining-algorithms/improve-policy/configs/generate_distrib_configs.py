__author__ = 'Thang'

for i in range(1):
    for j in range(5):
        with open('./edoc_' + str(i) + '.conf', 'r') as f_ori:
            with open('./edoc_' + str(i) + '_' + str(j) + '.conf', 'w') as f_out:
                for line in f_ori:
                    line = line.strip()
                    if 'numPoliciesPerSize=' in line:
                        f_out.write('numPoliciesPerSize=1\nrunPolicy=' + str(j) + '\n')
                    elif 'outputFile=' in line:
                        f_out.write('outputFile=../../output/scaling_f_' + str(i) + '/output_e-doc_75_' + str(j) + '.txt\n')
                    else:
                        f_out.write(line + '\n')

