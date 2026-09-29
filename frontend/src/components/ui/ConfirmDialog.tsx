import { AlertTriangle } from 'lucide-react'
import { Modal } from './Modal'
import { Button } from './Button'

interface Props {
  open: boolean
  title: string
  message: string
  confirmText?: string
  danger?: boolean
  loading?: boolean
  onConfirm: () => void
  onClose: () => void
}

export function ConfirmDialog({ open, title, message, confirmText = 'Confirmar', danger, loading, onConfirm, onClose }: Props) {
  return (
    <Modal open={open} onClose={onClose} title={title} size="sm"
      footer={<>
        <Button variant="secondary" onClick={onClose}>Cancelar</Button>
        <Button variant={danger ? 'danger' : 'primary'} loading={loading} onClick={onConfirm}>{confirmText}</Button>
      </>}>
      <div className="flex gap-3">
        <div className={`h-fit rounded-full p-2 ${danger ? 'bg-red-50 text-red-600' : 'bg-brand-50 text-brand-600'}`}><AlertTriangle className="h-5 w-5" /></div>
        <p className="text-sm leading-relaxed text-ink-600">{message}</p>
      </div>
    </Modal>
  )
}
