#!/usr/bin/env python3
import csv
import sys
from datetime import datetime

def convert_datetime(dt_string):
    """
    Convert datetime from format 'Dy Mon DD HH24:MI:SS HKT YYYY' 
    to 'YYYY-MM-DD HH24:MI:SS'
    
    Example: 'Wed Jan 15 14:30:45 HKT 2025' -> '2025-01-15 14:30:45'
    """
    try:
        # Parse the input format: "Dy Mon DD HH:MI:SS HKT YYYY"
        dt_obj = datetime.strptime(dt_string.strip(), "%a %b %d %H:%M:%S HKT %Y")
        # Return in the desired format: "YYYY-MM-DD HH:MI:SS"
        return dt_obj.strftime("%Y-%m-%d %H:%M:%S")
    except Exception as e:
        print(f"Error converting '{dt_string}': {e}", file=sys.stderr)
        return dt_string

def convert_csv(input_file, output_file):
    """Read CSV, convert DateTime column, and write to output CSV"""
    try:
        with open(input_file, 'r', encoding='utf-8') as infile, \
             open(output_file, 'w', encoding='utf-8', newline='') as outfile:
            
            reader = csv.DictReader(infile)
            if not reader.fieldnames:
                print("Error: CSV file is empty or invalid", file=sys.stderr)
                return False
            
            # Create writer with same fieldnames
            writer = csv.DictWriter(outfile, fieldnames=reader.fieldnames)
            writer.writeheader()
            
            # Process each row
            for row in reader:
                if 'DateTime' in row:
                    row['DateTime'] = convert_datetime(row['DateTime'])
                writer.writerow(row)
        
        print(f"Conversion complete. Output saved to: {output_file}")
        return True
    except FileNotFoundError:
        print(f"Error: Input file '{input_file}' not found", file=sys.stderr)
        return False
    except Exception as e:
        print(f"Error: {e}", file=sys.stderr)
        return False

if __name__ == "__main__":
    input_file = sys.argv[1] if len(sys.argv) > 1 else "input.csv"
    output_file = sys.argv[2] if len(sys.argv) > 2 else "output.csv"
    
    convert_csv(input_file, output_file)
