cd ../mining-algorithms/learning-data-generator
cd bin
java -Xms3g -Xmx3g -cp ".;../libs/commons-lang3-3.4.jar." learningdatagenerator/LearningDataGenerator ../configs/edoc_2.conf
cd ../..
cd DTRMU
python main.py ./configs/edoc_2.conf 
cd ../improve-policy/
cd bin
java -Xms3g -Xmx3g -cp ".;../libs/commons-math-2.2.jar;../libs/commons-lang3-3.4.jar;../libs/commons-math3-3.3.jar;../libs/jdi.jar." algo/ReBACMiner ../configs/edoc_2.conf
cd ../../..