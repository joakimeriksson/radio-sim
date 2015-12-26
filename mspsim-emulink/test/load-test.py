#!/usr/bin/python
import sys,os,subprocess
import xml.etree.ElementTree as ET

class Type(object):
    def __init__(self, mtype):
        self.desc = mtype.find('description').text
        self.id = mtype.find('identifier').text
        self.command = mtype.find('commands').text
        self.firmware = mtype.find('firmware').text
    def out(self):
        print "Type    :", self.desc," ID:",self.id
        print "Command :", self.command
        print "Firmware:", self.firmware
    def make(self,path):
        print "BUILDING IN", path, " CMD: ", self.command
        make_process = subprocess.Popen(self.command, shell=True, stderr=subprocess.STDOUT, cwd=path)
        if make_process.wait() != 0:
            print "ERROR!!!"
            sys.exit(1)

class Node(object):
    def __init__(self, mote):
        self.id = mote.find('interface_config/id').text
        self.x = mote.find('interface_config/x').text
        self.y = mote.find('interface_config/y').text
        self.type = mote.find('motetype_identifier').text

    def out(self):
        print "Node:",self.id,"of", self.type, "pos:", self.x,",",self.y

loadedTypes = {}

coojaFile = sys.argv[1]
path = os.path.dirname(os.path.abspath(coojaFile))

tree = ET.parse(coojaFile)
root = tree.getroot()
print "**** Simulation :", root.find('simulation/title').text, "****"
print "     RadioMedium:", root.find('simulation/radiomedium').text.strip()
print "     Sim Path   :", path
print "****"
# Load and build the types
types = tree.iter('motetype')
for type in types:
    t = Type(type)
    t.out()
    t.make(path)
    loadedTypes[t.id] = t
# Load the nodes
motes = root.iter('mote')
for mote in motes:
    n = Node(mote)
    n.out()
    print "Should load:",loadedTypes[n.type].firmware

