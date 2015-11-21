#!/bin/bash
DEVICE=$1
CHANNEL=$2
killall stm32w-wireshark
sleep 1
wireshark -k -i <( ./stm32w-wireshark $DEVICE $CHANNEL )

