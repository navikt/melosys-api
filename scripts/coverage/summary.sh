#!/bin/bash

# Coverage Summary Script
# Generates a simple one-line-per-module coverage report

echo "==================================================================="
echo "JaCoCo Coverage Summary"
echo "==================================================================="
printf "%-25s %10s %10s %10s\n" "Module" "Lines" "Branches" "Methods"
echo "-------------------------------------------------------------------"

total_line_covered=0
total_line_missed=0
total_branch_covered=0
total_branch_missed=0
total_method_covered=0
total_method_missed=0

# Find all jacoco.csv files and process them
for csv_file in */target/site/jacoco/jacoco.csv; do
    if [ -f "$csv_file" ]; then
        module=$(dirname $(dirname $(dirname $(dirname "$csv_file"))))
        
        # Sum up coverage data from CSV (skip header line)
        line_covered=$(awk -F',' 'NR>1 {sum+=$9} END {print sum}' "$csv_file")
        line_missed=$(awk -F',' 'NR>1 {sum+=$8} END {print sum}' "$csv_file")
        branch_covered=$(awk -F',' 'NR>1 {sum+=$7} END {print sum}' "$csv_file")
        branch_missed=$(awk -F',' 'NR>1 {sum+=$6} END {print sum}' "$csv_file")
        method_covered=$(awk -F',' 'NR>1 {sum+=$13} END {print sum}' "$csv_file")
        method_missed=$(awk -F',' 'NR>1 {sum+=$12} END {print sum}' "$csv_file")
        
        # Calculate percentages
        if [ $((line_covered + line_missed)) -gt 0 ]; then
            line_pct=$(awk "BEGIN {printf \"%.1f\", ($line_covered / ($line_covered + $line_missed)) * 100}")
        else
            line_pct="N/A"
        fi
        
        if [ $((branch_covered + branch_missed)) -gt 0 ]; then
            branch_pct=$(awk "BEGIN {printf \"%.1f\", ($branch_covered / ($branch_covered + $branch_missed)) * 100}")
        else
            branch_pct="N/A"
        fi
        
        if [ $((method_covered + method_missed)) -gt 0 ]; then
            method_pct=$(awk "BEGIN {printf \"%.1f\", ($method_covered / ($method_covered + $method_missed)) * 100}")
        else
            method_pct="N/A"
        fi
        
        printf "%-25s %9s%% %9s%% %9s%%\n" "$module" "$line_pct" "$branch_pct" "$method_pct"
        
        # Add to totals
        total_line_covered=$((total_line_covered + line_covered))
        total_line_missed=$((total_line_missed + line_missed))
        total_branch_covered=$((total_branch_covered + branch_covered))
        total_branch_missed=$((total_branch_missed + branch_missed))
        total_method_covered=$((total_method_covered + method_covered))
        total_method_missed=$((total_method_missed + method_missed))
    fi
done

echo "-------------------------------------------------------------------"

# Calculate total percentages
if [ $((total_line_covered + total_line_missed)) -gt 0 ]; then
    total_line_pct=$(awk "BEGIN {printf \"%.1f\", ($total_line_covered / ($total_line_covered + $total_line_missed)) * 100}")
else
    total_line_pct="N/A"
fi

if [ $((total_branch_covered + total_branch_missed)) -gt 0 ]; then
    total_branch_pct=$(awk "BEGIN {printf \"%.1f\", ($total_branch_covered / ($total_branch_covered + $total_branch_missed)) * 100}")
else
    total_branch_pct="N/A"
fi

if [ $((total_method_covered + total_method_missed)) -gt 0 ]; then
    total_method_pct=$(awk "BEGIN {printf \"%.1f\", ($total_method_covered / ($total_method_covered + $total_method_missed)) * 100}")
else
    total_method_pct="N/A"
fi

printf "%-25s %9s%% %9s%% %9s%%\n" "TOTAL" "$total_line_pct" "$total_branch_pct" "$total_method_pct"
echo "==================================================================="
