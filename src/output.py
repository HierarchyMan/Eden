import os
import os.path  # Import the os.path module


def collect_files(output_file="output.txt", extensions=(".java", ".yml", ".json", ".properties", ".gradle", ".conf"), exclude_dirs=None):
    """
    Traverses the current directory and its subdirectories,
    collects the contents of all files with specified extensions,
    and writes them to a single output file, separated by the filename.
    Excludes directories (and their subdirectories) whose names are in exclude_dirs.

    Args:
        output_file (str, optional): The name of the output file.
                                        Defaults to "output.txt".
        extensions (tuple, optional): A tuple of file extensions to collect.
                                         Defaults to (".java", ".yml", ".json").
        exclude_dirs (list, optional): A list of directory names to exclude.
                                         Defaults to None (no directories excluded).
    """

    if exclude_dirs is None:
        exclude_dirs = []  # Initialize to empty list if None is provided, to avoid errors

    try:
        with open(output_file, "w", encoding="utf-8") as outfile:  # Ensure proper encoding
            for root, dirs, files in os.walk("."):  # Start from the current directory
                # Check if the current directory's name is in the exclude list.
                # If it is, skip processing files and subdirectories within it.
                if os.path.basename(root) in exclude_dirs:
                    dirs[:] = []  # Clear the dirs list to prevent os.walk from descending into this directory
                    continue  # Skip to the next iteration of the loop

                for file in files:
                    if file.endswith(extensions):
                        file_path = os.path.join(root, file)
                        try:
                            with open(file_path, "r", encoding="utf-8") as infile:  # Ensure proper encoding
                                content = infile.read()
                                outfile.write(f"Filename: {file_path}\n")
                                outfile.write(content)
                                outfile.write("\n\n")

                        except FileNotFoundError:
                            print(f"Error: File not found: {file_path}")
                        except PermissionError:
                            print(f"Error: Permission denied: {file_path}")
                        except Exception as e:
                            print(f"Error reading file {file_path}: {e}")

    except Exception as e:
        print(f"Error writing to output file: {e}")



if __name__ == "__main__":
    # Example usage:  Exclude directories named "example1" or "example2"
    exclude_directories = ["example1", "example2"]
    collect_files(exclude_dirs=exclude_directories)
    print(f"Java, YAML, and JSON files collected (excluding directories {exclude_directories}) and written to output.txt")
