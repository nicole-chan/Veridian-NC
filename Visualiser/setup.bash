#!/bin/bash

# Nicole Chan (nc83@students.waikato.ac.nz)

export DLC_NEWSFRAME_HOME=`pwd`
export PATH=$DLC_NEWSFRAME_HOME/script:$PATH

if [ -z $CLASSPATH ] ; then
	export CLASSPATH=$DLC_NEWSFRAME_HOME/src
else
	export CLASSPATH=$DLC_NEWSFRAME_HOME/src:$CLASSPATH
fi

echo "Set DLC_NEWSFRAME_HOME to: $DLC_NEWSFRAME_HOME"
echo "Updated PATH to include DLC_NEWSFRAME_HOME/script"
echo "Updated CLASSPATH to include DLC_NEWSFRAME_HOME/src"
