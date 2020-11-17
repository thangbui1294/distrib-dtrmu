__author__ = 'Thang'

for i in range(4):
    for j in range(5):
        with open('./eWorkforce_' + str(i) + '.conf', 'r') as f_ori:
            with open('./eWorkforce_' + str(i) + '_' + str(j) + '.conf', 'w') as f_out:
                for line in f_ori:
                    line = line.strip()
                    if 'num_policies=' in line:
                        f_out.write('num_policies=1\nrun_policy=' + str(j) + '\n')
                    elif 'output_file=' in line:
                        f_out.write('output_file=../output/scaling_f_' + str(i) + '/output_eWorkforce_10' + '_' + str(j) + '.txt\n')
                    else:
                        f_out.write(line + '\n')

