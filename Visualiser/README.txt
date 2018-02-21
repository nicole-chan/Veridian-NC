Veridian Project
Nicole Chan (nc83@students.waikato.ac.nz)

HOW TO RUN:
1. Run setup.bash to set up environment.
	. ./setup.bash
2. Run compile.sh to compile all the .java files in the home directory/src
	./compile.sh
3. Run weka_rf.sh
	e.g. weka_rf.sh ~/veridian/CambridgeSentinel/1906/11/03_01/ run_RandForest.sh test-Full.csv -V 1 ~/veridian/CambridgeSentinel/1906/11/10_01/
	e.g. weka_rf.sh ~/veridian/test/ run_RandForest.sh test-Full.csv -V 1

	
	NOTE:
	 - Should be in the format: weka_rf.sh first_dir algorithm_name
	 	- Then optional items in order: output_file_name verbosity_flag verbosity_value more_directories
 	- Order could be modified if the weka_rf.sh script's error checking changes.

