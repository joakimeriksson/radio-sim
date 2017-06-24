#!/usr/bin/python
import sys,os,subprocess,time,re
import xml.etree.ElementTree as ET

class Type(object):
    def __init__(self, mtype):
        self.desc = mtype.find('description').text
        self.id = mtype.find('identifier').text
        self.command = mtype.find('commands').text
        self.firmware = mtype.find('firmware').text
        self.nodes = []
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
        self.z = 0.0
        self.type = mote.find('motetype_identifier').text

    def yaml(self):
        return "{id: %s, position: [%s,%s,%s]}" % (self.id, self.x, self.y, self.z)
    def __repr__(self):
        return "Node: " + self.id + " of " + self.type + " pos: " + self.x + "," + self.y


def cooja_load(filename,target=''):
    loadedTypes = {}
    allNodes = {}
    coojaFile = filename
    path = os.path.dirname(os.path.abspath(coojaFile))
    # find Contiki in the path - TODO: this must be configurable...
    contiki = path[:path.find("contiki") + len("contiki")]
    tree = ET.parse(coojaFile)
    root = tree.getroot()
    print "**** Simulation :", root.find('simulation/title').text, "****"
    print "     RadioMedium:", root.find('simulation/radiomedium').text.strip()
    print "     Sim Path   :", path
    print "     Contiki    :", contiki
    print "****"
    # Load and build the types
    root = tree.getroot()
    types = root.iter('motetype')
    for type in types:
        t = Type(type)
        t.out()
        print "Target:", target
        if target != '':
            t.command = re.sub("TARGET=(.+?)( |$)", "TARGET=" + target + " ", t.command)
            t.command = re.sub("\.(.+?)( |$)", "." + target + " ", t.command)
            t.firmware = re.sub("\.(.+?)( |$)", "." + target, t.firmware)
            print "New CMD: " + t.command
            print "New  FW: " + t.firmware
        t.make(path)
        loadedTypes[t.id] = t
    # Load the nodes
    root = tree.getroot()
    motes = root.iter('mote')
    for mote in motes:
        n = Node(mote)
        print n
        allNodes[n.id] = n
        loadedTypes[n.type].nodes.append(n)

    root = tree.getroot()
    script = root.find('plugin/plugin_config/script').text
    lcount = script.count('\n')
    print " === SCRIPT === (",lcount,")"
    print script
    print " =============="
    return (loadedTypes, allNodes, lcount, script, contiki)
