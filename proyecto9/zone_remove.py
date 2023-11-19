
##python zone_remove.py

#get folders in directory animal/animal
import os
import shutil
import sys
import time
import datetime
import random
import string
import subprocess
import re
import json
import requests

path = "proyecto9/animals/animals"
current_dir = os.getcwd()
print(current_dir)
dirs = os.listdir( path )

# This would print all the files and directories
for folder in dirs:
    #get files in folder
    files = os.listdir(path+"/"+folder)
     #if file ends in .jpg keep it, else delete it
    for file in files:
        if file.endswith("Zone.Identifier"):
            print("deleting file: "+file)
            os.remove(path+"/"+folder+"/"+file)
            
        else:
            print("keeping file: "+file)
            continue