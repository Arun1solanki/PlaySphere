import type { InputHTMLAttributes, ReactNode, TextareaHTMLAttributes } from 'react'

type BaseProps = {
  label: string
  error?: string
  hint?: string
  icon?: ReactNode
}

type InputProps = BaseProps & InputHTMLAttributes<HTMLInputElement> & { multiline?: false }
type TextareaProps = BaseProps & TextareaHTMLAttributes<HTMLTextAreaElement> & { multiline: true }

export function FormField(props: InputProps | TextareaProps) {
  const { label, error, hint, icon, multiline, ...fieldProps } = props
  return (
    <label className={`form-field ${error ? 'has-error' : ''}`}>
      <span className="form-label">{label}</span>
      <span className="input-wrap">
        {icon && <span className="input-icon">{icon}</span>}
        {multiline ? (
          <textarea {...(fieldProps as TextareaHTMLAttributes<HTMLTextAreaElement>)} />
        ) : (
          <input {...(fieldProps as InputHTMLAttributes<HTMLInputElement>)} />
        )}
      </span>
      {error ? <span className="field-error">{error}</span> : hint ? <span className="field-hint">{hint}</span> : null}
    </label>
  )
}
