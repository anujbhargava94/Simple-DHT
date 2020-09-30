# Simple-DHT

## Objective
Implemeted a simple DHT based on Chord. Although the design is based on Chord, it is a simplified version of Chord; 
I didn't implement finger tables and finger-based routing; I didn't handle node leaves/failures. 
Therefore, there are three things I implemented: 1) ID space partitioning/re-partitioning, 2) Ring-based routing, and 3) Node joins.

## Introduction

Implemented a scalable distributed lookup protocol that addressed the problem of peer-to-peer applications to efficiently 
locate the node that stores a particular data item.

## Implementation

### LDump
a. When touched, this button dump and display all the <key, value> pairs
stored in local partition of the node.​
b. This means that this button can give @ as the selection parameter to query().
### GDump
a. When touched, this button dump and display all the <key, value> pairs
stored in your ​whole​ DHT. Thus, LDump button is for local dump, and this button
(GDump) is for global dump of the entire <key, value> pairs.
b. This means that this button can give * as the selection parameter to query().

