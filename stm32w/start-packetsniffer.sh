#!/bin/bash
killall stm32w-wireshark
sleep 1
wireshark -k -i <( ./stm32w-wireshark /dev/ttyACM0 14 )

