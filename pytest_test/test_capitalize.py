# test_capitalize.py

import pytest
import subprocess

def call_func(x):
    return subprocess.call(x, shell=True)

def test_answer_false():
    assert call_func("exit 1") == 0

def test_answer_pass():
    assert call_func("ls -la") == 0

def test_answer_cmd1():
    assert call_func("echo 1") == 0

def test_answer_cmd2():
    assert call_func("pwd") == 0

def test_answer_cmd3():
    assert call_func("ls -l") == 0

def test_answer_cmd4():
    assert call_func("echo 'a'") == 0

def test_answer_cmd5():
    assert call_func("echo '123332'") == 0
