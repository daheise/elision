#!/bin/bash
#make aliases work in scripts
shopt -s expand_aliases
alias stripcolors='sed -r "s/\x1B\[([0-9]{1,2}(;[0-9]{1,2})?)?[mGK]//g"'

function check_failure() {
fail_lines=$(echo "$1" | stripcolors | egrep -o 'fails -> [[:digit:]]+')
#IFS=$"\n"
while read i
do
fail_number=$(echo "$i" | perl -pe 's/fails[\s]->[\s]([\d]+)/$1/')
  if [ ! $fail_number = "0" ]
    then
    echo "$fail_number failures reported."
    exit $fail_number
  fi
done <<< "$fail_lines"
}

pushd Elision
#eli_run=$(echo 'inc("check/Matching.eli")' | ./elision.sh)
echo "Starting elision checks..."
echo "Running matching tests..."
check_failure "$(echo 'inc("check/Matching.eli")' | ./elision.sh)"
stty sane
echo "Running math tests..."
check_failure "$(echo 'inc("check/MathTest.eli")' | ./elision.sh)"
stty sane
echo "Running AC matching test #1..."
check_failure "$(echo 'inc("check/ac-speed3.eli")' | ./elision.sh)"
stty sane
echo "Running AC matching test #2..."
check_failure "$(echo 'inc("check/ac-speed-reorder.eli")' | ./elision.sh)"
stty sane
echo "Running AC matching test #3..."
check_failure "$(echo 'inc("check/ac-topfast.eli")' | ./elision.sh)"
stty sane
echo "Running AC matching test #4..."
check_failure "$(echo 'inc("check/ac-topslow.eli")' | ./elision.sh)"
stty sane
echo "Elision checks completed."
popd
stty sane
