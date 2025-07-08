'use client';

import { forwardRef, useImperativeHandle, useRef } from 'react';
import Editor, { type Monaco } from '@monaco-editor/react';
import type { editor } from 'monaco-editor';
import { cn } from '@/lib/utils';

export interface CodeEditorRef {
  getValue: () => string;
  setValue: (value: string) => void;
  focus: () => void;
}

interface CodeEditorProps {
  value?: string;
  defaultValue?: string;
  onChange?: (value: string | undefined) => void;
  language?: string;
  height?: string | number;
  placeholder?: string;
  readOnly?: boolean;
  className?: string;
  theme?: 'vs-dark' | 'light';
}

export const CodeEditor = forwardRef<CodeEditorRef, CodeEditorProps>(
  ({
    value,
    defaultValue,
    onChange,
    language = 'yaml',
    height = 300,
    placeholder,
    readOnly = false,
    className,
    theme = 'vs-dark'
  }, ref) => {
    const editorRef = useRef<editor.IStandaloneCodeEditor | null>(null);

    useImperativeHandle(ref, () => ({
      getValue: () => {
        return editorRef.current?.getValue() ?? '';
      },
      setValue: (newValue: string) => {
        editorRef.current?.setValue(newValue);
      },
      focus: () => {
        editorRef.current?.focus();
      },
    }));

    const handleEditorDidMount = (editorInstance: editor.IStandaloneCodeEditor, _monaco: Monaco) => {
      editorRef.current = editorInstance;

      if (placeholder && !value && !defaultValue) {
        editorInstance.setValue(placeholder);
        editorInstance.setSelection({ 
          startLineNumber: 1, 
          startColumn: 1, 
          endLineNumber: 1, 
          endColumn: placeholder.length + 1 
        });
      }
    };

    const handleEditorChange = (newValue: string | undefined) => {
      onChange?.(newValue);
    };

    return (
      <div className={cn('border border-border rounded-md overflow-hidden', className)}>
        <Editor
          height={height}
          defaultLanguage={language}
          value={value}
          defaultValue={defaultValue}
          onChange={handleEditorChange}
          onMount={handleEditorDidMount}
          theme={theme}
          options={{
            readOnly,
            minimap: { enabled: false },
            lineNumbers: 'on',
            roundedSelection: false,
            scrollBeyondLastLine: false,
            automaticLayout: true,
            tabSize: 2,
            insertSpaces: true,
            wordWrap: 'off',
            scrollbar: {
              horizontal: 'visible',
              vertical: 'visible',
            },
            fontSize: 14,
            fontFamily: 'ui-monospace, SFMono-Regular, "SF Mono", Monaco, Consolas, "Liberation Mono", "Courier New", monospace',
            folding: true,
            foldingStrategy: 'indentation',
            bracketPairColorization: {
              enabled: true,
            },
            autoIndent: 'full',
            formatOnPaste: true,
            formatOnType: true,
            renderWhitespace: 'selection',
            cursorBlinking: 'blink',
          }}
        />
      </div>
    );
  }
);

CodeEditor.displayName = 'CodeEditor'; 